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

    private final PillService pillService;
    private final DrugInfoRepository drugInfoRepository;

    @Override
    public void run(String... args) throws Exception {
        PillService.PillPage firstPage = pillService.getAllPillsPage(1, NUM_OF_ROWS);
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

    private int saveAll(List<PillSearchResponse> items) {
        List<DrugInfo> entities = items.stream()
                .map(this::toDrugInfo)
                .toList();
        drugInfoRepository.saveAll(entities);
        return entities.size();
    }

    private DrugInfo toDrugInfo(PillSearchResponse pill) {
        return DrugInfo.builder()
                .itemSeq(pill.getItemSeq())
                .name(pill.getItemName())
                .shape(pill.getDrugShape())
                .color(pill.getColorClass())
                .imprint(PillService.combineImprint(pill.getPrintFront(), pill.getPrintBack()))
                .drugType(pill.getEtcOtcName())
                .entpName(pill.getEntpName())
                .itemImage(pill.getItemImage())
                .build();
    }
}
