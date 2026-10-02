package com.tadaktadak.eunggeubi.domain.drug.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.PrmsnDetailResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 의약품 제품 허가정보(전체 42,707건)로 DrugInfo 테이블의 공백을 메우는 1회성 배치.
// e약은요는 전문의약품 데이터가 아예 없어서(실측: 전문의약품 19,813건 전부 효능정보 0건),
// 이 API로 그 공백만 채운다 — 새 행을 추가하지 않고, 이미 DB에 있는 행 중 efficacy/useInfo/
// cautionSections/drugType이 비어있는 것만 채우는 "gap-fill" 전용 배치다(EDrugIndexingRunner처럼
// 기존 값을 덮어쓰지 않음). e약은요 쪽이 더 읽기 쉬운 평문이고 이 API는 중첩 XML을 파싱해야
// 해서 품질이 들쭉날쭉할 수 있어, 이미 값이 있는 행은 건드리지 않는 쪽을 택했다.
// 사용상주의사항은 평문 caution 컬럼 대신 항목별로 쪼갠 cautionSections(JSON)에만 채운다.
// 실행: --spring.profiles.active=local,drug-prmsn-index
@Slf4j
@Component
@Profile("drug-prmsn-index")
@RequiredArgsConstructor
public class DrugPrmsnIndexingRunner implements CommandLineRunner {

    // numOfRows 상한 자체는 500이지만, 이 API는 응답 한 건마다 주의사항 등 텍스트가 수천~수만 자라
    // 500건씩 받으면 응답이 무거워져 읽기 타임아웃(10s)에 자주 걸린다(실측: 후반부로 갈수록 실패율
    // 급증). 요청을 가볍게 만들어 타임아웃을 줄이려고 100건으로 낮춰 잡는다.
    private static final int NUM_OF_ROWS = 100;
    private static final long REQUEST_INTERVAL_MS = 200;
    // 첫 페이지는 전체 건수를 몰라 이후 페이지처럼 건너뛸 수 없어서, 실패 시 몇 번 재시도한다
    // (이번 세션에 실제로 첫 페이지 타임아웃 한 번으로 배치 전체가 바로 크래시하는 걸 겪음).
    private static final int FIRST_PAGE_MAX_RETRIES = 3;
    private static final long FIRST_PAGE_RETRY_DELAY_MS = 2000;

    private final DrugPrmsnService drugPrmsnService;
    private final DrugInfoRepository drugInfoRepository;

    @Value("${drug-prmsn.index.max-pages:0}")
    private int maxPages;

    @Value("${drug-prmsn.index.start-page:1}")
    private int startPage;

    @Override
    public void run(String... args) throws Exception {
        DrugPrmsnService.PrmsnPage firstPage = fetchFirstPage();
        int totalCount = firstPage.totalCount();
        int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);
        if (maxPages > 0 && startPage + maxPages - 1 < totalPages) {
            totalPages = startPage + maxPages - 1;
            log.info("drug-prmsn.index.max-pages={} 설정으로 {}페이지까지만 수집합니다.", maxPages, totalPages);
        }
        log.info("의약품 제품 허가정보 공백 채우기 시작: 총 {}건, {}페이지부터 {}페이지까지 수집 예정",
                totalCount, startPage, totalPages);

        int filledCount = fillGaps(firstPage.items());
        int failedPages = 0;

        for (int pageNo = startPage + 1; pageNo <= totalPages; pageNo++) {
            try {
                DrugPrmsnService.PrmsnPage page = drugPrmsnService.getAllPrmsnDetailPage(pageNo, NUM_OF_ROWS);
                filledCount += fillGaps(page.items());
                log.info("[{}/{}] 페이지 처리 완료 (이번 실행 누적 {}건 채움)", pageNo, totalPages, filledCount);
            } catch (Exception e) {
                failedPages++;
                log.error("[{}/{}] 페이지 수집 실패, 건너뜀", pageNo, totalPages, e);
            }

            Thread.sleep(REQUEST_INTERVAL_MS);
        }

        log.info("의약품 제품 허가정보 공백 채우기 완료: 이번 실행에서 {}건 채움, 실패 페이지 {}개", filledCount, failedPages);
    }

    private DrugPrmsnService.PrmsnPage fetchFirstPage() throws InterruptedException {
        for (int attempt = 1; attempt <= FIRST_PAGE_MAX_RETRIES; attempt++) {
            try {
                return drugPrmsnService.getAllPrmsnDetailPage(startPage, NUM_OF_ROWS);
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

    private int fillGaps(List<PrmsnDetailResponse> items) {
        List<String> itemSeqs = items.stream().map(PrmsnDetailResponse::getItemSeq).toList();
        Map<String, DrugInfo> existing = drugInfoRepository.findAllById(itemSeqs).stream()
                .collect(Collectors.toMap(DrugInfo::getItemSeq, Function.identity()));

        List<DrugInfo> toUpdate = new ArrayList<>();
        for (PrmsnDetailResponse item : items) {
            DrugInfo info = existing.get(item.getItemSeq());
            if (info == null) {
                continue; // DB에 없는 약은 이번 배치 대상이 아니다(신규 행 추가 안 함)
            }
            DrugInfo merged = mergeIfNeeded(info, item);
            if (merged != null) {
                toUpdate.add(merged);
            }
        }

        if (!toUpdate.isEmpty()) {
            drugInfoRepository.saveAll(toUpdate);
        }
        return toUpdate.size();
    }

    // DB에 이미 있는 행 중, 비어있는 필드가 하나라도 있고 이번 응답이 그걸 채울 수 있을 때만
    // 병합한 엔티티를 반환한다. 이미 값이 있는 필드는 절대 덮어쓰지 않는다. 채울 게 없으면 null
    // 반환(저장 대상에서 제외돼 불필요한 쓰기를 줄인다).
    private DrugInfo mergeIfNeeded(DrugInfo info, PrmsnDetailResponse prmsn) {
        if (prmsn == null) {
            return null;
        }

        boolean canFillEfficacy = info.getEfficacy() == null && prmsn.getEfficacy() != null;
        boolean canFillUseInfo = info.getUseInfo() == null && prmsn.getUseInfo() != null;
        // 평문 caution은 더 이상 이 배치가 채우지 않는다 — 구조화된 cautionSections로 대체됨.
        boolean canFillCautionSections = info.getCautionSections() == null && prmsn.getCautionSectionsJson() != null;
        boolean canFillDrugType = info.getDrugType() == null && prmsn.getDrugType() != null;

        if (!canFillEfficacy && !canFillUseInfo && !canFillCautionSections && !canFillDrugType) {
            return null;
        }

        DrugInfo.DrugInfoBuilder builder = info.toBuilder();
        if (canFillEfficacy) builder.efficacy(prmsn.getEfficacy());
        if (canFillUseInfo) builder.useInfo(prmsn.getUseInfo());
        if (canFillCautionSections) builder.cautionSections(prmsn.getCautionSectionsJson());
        if (canFillDrugType) builder.drugType(prmsn.getDrugType());
        return builder.build();
    }
}
