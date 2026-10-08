package com.tadaktadak.eunggeubi.domain.drug.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 낱알식별 API 전체(약 25,500건)를 미리 DrugInfo 테이블에 적재하는 1회성 배치.
// 이 API는 모양/색상/각인을 검색 조건으로 지원하지 않아서(PillService.searchPills 주석 참고),
// 전체 데이터를 우리 DB로 가져와 우리 쪽에서 조건 검색을 하는 방식으로 우회한다.
// 매 부팅마다 돌면 안 되므로 --spring.profiles.active=local,pill-index 로 명시적으로 켤 때만 실행된다.
// 요청 사이 딜레이는 API 트래픽 제한을 피하기 위함.
@Slf4j
@Component
@Profile("pill-index")
@RequiredArgsConstructor
public class PillInfoIndexingRunner implements CommandLineRunner {

    // 이 API의 numOfRows 최대치가 500이라 직접 테스트해서 확인함.
    private static final int NUM_OF_ROWS = 500;
    private static final long REQUEST_INTERVAL_MS = 200;
    // 첫 페이지는 전체 건수를 몰라 이후 페이지처럼 건너뛸 수 없어서, 실패 시 몇 번 재시도한다
    // (일시적 타임아웃/연결 끊김으로 배치 전체가 바로 크래시하는 일을 실측으로 겪음).
    private static final int FIRST_PAGE_MAX_RETRIES = 3;
    private static final long FIRST_PAGE_RETRY_DELAY_MS = 2000;

    private final PillService pillService;
    private final DrugInfoRepository drugInfoRepository;

    @Override
    public void run(String... args) throws Exception {
        PillService.PillPage firstPage = fetchFirstPage();
        int totalCount = firstPage.totalCount();
        int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);
        log.info("낱알식별 데이터 적재 시작: 총 {}건, {}페이지", totalCount, totalPages);

        int savedCount = saveAll(firstPage.items());
        int failedPages = 0;

        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            try {
                PillService.PillPage page = pillService.getAllPillsPage(pageNo, NUM_OF_ROWS);
                savedCount += saveAll(page.items());
                log.info("[{}/{}] 페이지 저장 완료 (누적 {}건)", pageNo, totalPages, savedCount);
            } catch (Exception e) {
                // 한 페이지가 실패해도 전체를 멈추지 않는다. 이 러너를 나중에 다시 돌리면
                // itemSeq가 같은 약은 갱신될 뿐이라 재시도 부담이 크지 않다.
                failedPages++;
                log.error("[{}/{}] 페이지 수집 실패, 건너뜀", pageNo, totalPages, e);
            }

            Thread.sleep(REQUEST_INTERVAL_MS);
        }

        log.info("낱알식별 데이터 적재 완료: {}건 저장, 실패 페이지 {}개", savedCount, failedPages);
    }

    private PillService.PillPage fetchFirstPage() throws InterruptedException {
        for (int attempt = 1; attempt <= FIRST_PAGE_MAX_RETRIES; attempt++) {
            try {
                return pillService.getAllPillsPage(1, NUM_OF_ROWS);
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

    // 이미 DB에 있는 행(e약은요/의약품 제품 허가정보로 채워진 efficacy/useInfo/caution/
    // cautionSections 포함)은 보존하고 낱알식별 소스 필드만 덮어쓴다. 예전엔 새 DrugInfo를
    // 통째로 새로 만들어 저장해서, 이 배치를 재실행하면 다른 배치가 채운 데이터가 전부 null로
    // 지워지는 문제가 있었다(실측으로 확인됨).
    private int saveAll(List<PillSearchResponse> items) {
        List<String> itemSeqs = items.stream().map(PillSearchResponse::getItemSeq).toList();
        Map<String, DrugInfo> existing = drugInfoRepository.findAllById(itemSeqs).stream()
                .collect(Collectors.toMap(DrugInfo::getItemSeq, Function.identity()));

        List<DrugInfo> entities = items.stream()
                .map(pill -> toDrugInfo(pill, existing.get(pill.getItemSeq())))
                .toList();
        drugInfoRepository.saveAll(entities);
        return entities.size();
    }

    private DrugInfo toDrugInfo(PillSearchResponse pill, DrugInfo prev) {
        DrugInfo.DrugInfoBuilder builder = prev != null
                ? prev.toBuilder()
                : DrugInfo.builder().itemSeq(pill.getItemSeq());

        return builder
                .name(pill.getItemName())
                .shape(pill.getDrugShape())
                .color(pill.getColorClass())
                .imprint(pill.getImprint())
                .drugType(pill.getEtcOtcName())
                .entpName(pill.getEntpName())
                .itemImage(pill.getItemImage())
                .build();
    }
}
