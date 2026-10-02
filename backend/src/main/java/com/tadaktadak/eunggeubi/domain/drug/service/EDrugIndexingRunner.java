package com.tadaktadak.eunggeubi.domain.drug.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// e약은요 API 전체(약 4,800건)를 미리 DrugInfo 테이블에 적재하는 1회성 배치.
// PillInfoIndexingRunner/DurInteractionIndexingRunner와 같은 이유로, 검색/상세조회를
// 매번 외부 API에 의존하지 않고 우리 DB만으로 처리할 수 있게 한다.
// itemSeq가 낱알식별 적재분과 겹치는 경우가 많아 기존 shape/color/imprint/drugType/
// entpName/itemImage는 보존하고 name/efficacy/useInfo/caution만 덮어쓴다(부분 병합).
// 실행: --spring.profiles.active=local,edrug-index
//
// 전체 규모가 작아(4,800여 건 / 500 = 10페이지) 보통은 한 번에 끝나지만, 혹시 중간에
// 멈추면 edrug.index.start-page로 이어서 할 수 있다(upsert라 재실행해도 안전).
@Slf4j
@Component
@Profile("edrug-index")
@RequiredArgsConstructor
public class EDrugIndexingRunner implements CommandLineRunner {

    // 직접 테스트해서 확인한 이 API의 numOfRows 상한.
    private static final int NUM_OF_ROWS = 500;
    private static final long REQUEST_INTERVAL_MS = 200;
    // 첫 페이지는 전체 건수를 몰라 이후 페이지처럼 건너뛸 수 없어서, 실패 시 몇 번 재시도한다
    // (일시적 타임아웃/연결 끊김으로 배치 전체가 바로 크래시하는 일을 실측으로 겪음).
    private static final int FIRST_PAGE_MAX_RETRIES = 3;
    private static final long FIRST_PAGE_RETRY_DELAY_MS = 2000;

    private final DrugService drugService;
    private final DrugInfoRepository drugInfoRepository;

    @Value("${edrug.index.max-pages:0}")
    private int maxPages;

    @Value("${edrug.index.start-page:1}")
    private int startPage;

    @Override
    public void run(String... args) throws Exception {
        DrugService.DrugPage firstPage = fetchFirstPage();
        int totalCount = firstPage.totalCount();
        int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);
        if (maxPages > 0 && startPage + maxPages - 1 < totalPages) {
            totalPages = startPage + maxPages - 1;
            log.info("edrug.index.max-pages={} 설정으로 {}페이지까지만 수집합니다.", maxPages, totalPages);
        }
        log.info("e약은요 데이터 적재 시작: 총 {}건, {}페이지부터 {}페이지까지 수집 예정",
                totalCount, startPage, totalPages);

        int savedCount = saveAll(firstPage.items());
        int failedPages = 0;

        for (int pageNo = startPage + 1; pageNo <= totalPages; pageNo++) {
            try {
                DrugService.DrugPage page = drugService.getAllDrugsPage(pageNo, NUM_OF_ROWS);
                savedCount += saveAll(page.items());
                log.info("[{}/{}] 페이지 저장 완료 (이번 실행 누적 {}건)", pageNo, totalPages, savedCount);
            } catch (Exception e) {
                failedPages++;
                log.error("[{}/{}] 페이지 수집 실패, 건너뜀", pageNo, totalPages, e);
            }

            Thread.sleep(REQUEST_INTERVAL_MS);
        }

        log.info("e약은요 데이터 적재 완료: 이번 실행에서 {}건 저장, 실패 페이지 {}개", savedCount, failedPages);
    }

    private DrugService.DrugPage fetchFirstPage() throws InterruptedException {
        for (int attempt = 1; attempt <= FIRST_PAGE_MAX_RETRIES; attempt++) {
            try {
                return drugService.getAllDrugsPage(startPage, NUM_OF_ROWS);
            } catch (Exception e) {
                if (attempt == FIRST_PAGE_MAX_RETRIES) {
                    throw e;
                }
                log.warn("첫 페이지 수집 실패({}번째 시도), {}ms 후 재시도", attempt, FIRST_PAGE_RETRY_DELAY_MS, e);
                Thread.sleep(FIRST_PAGE_RETRY_DELAY_MS);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private int saveAll(List<DrugInfoResponse> items) {
        List<String> itemSeqs = items.stream().map(DrugInfoResponse::getItemSeq).toList();
        Map<String, DrugInfo> existing = drugInfoRepository.findAllById(itemSeqs).stream()
                .collect(Collectors.toMap(DrugInfo::getItemSeq, Function.identity()));

        List<DrugInfo> merged = items.stream()
                .map(item -> toDrugInfo(item, existing.get(item.getItemSeq())))
                .toList();
        drugInfoRepository.saveAll(merged);
        return merged.size();
    }

    private DrugInfo toDrugInfo(DrugInfoResponse item, DrugInfo prev) {
        DrugInfo.DrugInfoBuilder builder = prev != null
                ? prev.toBuilder()
                : DrugInfo.builder().itemSeq(item.getItemSeq());

        // e약은요 응답에 해당 필드가 비어있으면(필드 누락 등, getTextOrNull이 흔히 만드는 케이스)
        // null로 무조건 덮어쓰지 않는다 — 예전엔 무조건 덮어써서, 의약품 제품 허가정보 배치가
        // 먼저 채워둔 efficacy/useInfo를 이 배치 재실행이 지워버리는 문제가 있었다(실측 확인).
        // name은 필수 필드라 값이 있을 때만 갱신한다(itemImage와 같은 이유로 보존 우선).
        if (item.getName() != null) builder.name(item.getName());
        if (item.getEfficacy() != null) builder.efficacy(item.getEfficacy());
        if (item.getUseInfo() != null) builder.useInfo(item.getUseInfo());
        if (item.getCaution() != null) builder.caution(item.getCaution());

        // 외형/구분 정보는 낱알식별 쪽이 더 정확하니 이미 있으면 건드리지 않는다.
        if (prev == null || prev.getItemImage() == null) {
            builder.itemImage(item.getItemImage());
        }

        return builder.build();
    }
}
