package com.tadaktadak.eunggeubi.domain.drug.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.DurTabooResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInteraction;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

// DUR 병용금기 API 전체를 미리 DrugInteraction 테이블에 적재하는 1회성 배치.
// PillInfoIndexingRunner와 같은 이유(실시간 조합 체크가 느리거나 안 될 수 있어서)로
// 전체를 우리 DB로 가져와 우리 쪽에서 조건 검색을 하는 방식을 쓴다.
// DrugInfo와 다르게 itemSeq 하나로 유일하게 정해지는 게 아니라 "쌍" 데이터라서
// upsert 대신 매번 전체를 지우고 새로 채운다(재실행해도 안전).
// 실행: --spring.profiles.active=local,dur-index
//
// 전체 803,030건 / 500 = 1,607페이지라 실제로 다 받으면 30~50분 정도 걸린다.
// dur.index.max-pages로 앞에서부터 일부만 받아 소량 검증할 수 있다(기본 0 = 전체).
// 중간에 멈췄다가 이어서 할 땐 dur.index.start-page를 쓴다. start-page가 2 이상이면
// 기존 데이터를 지우지 않고(이미 저장된 앞부분을 보존) 그 페이지부터 이어서 수집한다.
@Slf4j
@Component
@Profile("dur-index")
@RequiredArgsConstructor
public class DurInteractionIndexingRunner implements CommandLineRunner {

    // 직접 테스트해서 확인한 이 API의 numOfRows 상한.
    private static final int NUM_OF_ROWS = 500;
    private static final long REQUEST_INTERVAL_MS = 200;
    // 첫 페이지는 전체 건수를 몰라 이후 페이지처럼 건너뛸 수 없어서, 실패 시 몇 번 재시도한다
    // (일시적 타임아웃/연결 끊김으로 배치 전체가 바로 크래시하는 일을 실측으로 겪음).
    private static final int FIRST_PAGE_MAX_RETRIES = 3;
    private static final long FIRST_PAGE_RETRY_DELAY_MS = 2000;

    private final DurService durService;
    private final DrugInteractionRepository drugInteractionRepository;

    @Value("${dur.index.max-pages:0}")
    private int maxPages;

    @Value("${dur.index.start-page:1}")
    private int startPage;

    @Override
    public void run(String... args) throws Exception {
        boolean resuming = startPage > 1;

        if (resuming) {
            log.info("dur.index.start-page={} 설정으로 기존 데이터를 지우지 않고 이어서 수집합니다.", startPage);
        }

        // 첫 페이지를 먼저 성공적으로 받은 뒤에 지운다 — 삭제부터 하고 첫 페이지를 받다가
        // 실패하면(재시도까지 다 소진) 기존 데이터만 날아가고 새 데이터는 하나도 안 들어간
        // 상태로 끝나버리는 문제가 있었다.
        DurService.DurPage firstPage = fetchFirstPage();
        if (!resuming) {
            drugInteractionRepository.deleteAllInBatch();
            log.info("기존 병용금기 데이터 삭제 완료, 재적재 시작");
        }
        int totalCount = firstPage.totalCount();
        int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);
        if (maxPages > 0 && startPage + maxPages - 1 < totalPages) {
            totalPages = startPage + maxPages - 1;
            log.info("dur.index.max-pages={} 설정으로 {}페이지까지만 수집합니다.", maxPages, totalPages);
        }
        log.info("DUR 병용금기 데이터 적재 시작: 총 {}건, {}페이지부터 {}페이지까지 수집 예정",
                totalCount, startPage, totalPages);

        int savedCount = saveAll(firstPage.items());
        int failedPages = 0;

        for (int pageNo = startPage + 1; pageNo <= totalPages; pageNo++) {
            try {
                DurService.DurPage page = durService.getAllTabooPage(pageNo, NUM_OF_ROWS);
                savedCount += saveAll(page.items());
                log.info("[{}/{}] 페이지 저장 완료 (이번 실행 누적 {}건)", pageNo, totalPages, savedCount);
            } catch (Exception e) {
                failedPages++;
                log.error("[{}/{}] 페이지 수집 실패, 건너뜀", pageNo, totalPages, e);
            }

            Thread.sleep(REQUEST_INTERVAL_MS);
        }

        log.info("DUR 병용금기 데이터 적재 완료: 이번 실행에서 {}건 저장, 실패 페이지 {}개", savedCount, failedPages);
    }

    private DurService.DurPage fetchFirstPage() throws InterruptedException {
        for (int attempt = 1; attempt <= FIRST_PAGE_MAX_RETRIES; attempt++) {
            try {
                return durService.getAllTabooPage(startPage, NUM_OF_ROWS);
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

    private int saveAll(List<DurTabooResponse> items) {
        List<DrugInteraction> entities = items.stream()
                .map(this::toDrugInteraction)
                .toList();
        drugInteractionRepository.saveAll(entities);
        return entities.size();
    }

    private DrugInteraction toDrugInteraction(DurTabooResponse dur) {
        return DrugInteraction.builder()
                .itemSeq(dur.getItemSeq())
                .itemName(dur.getItemName())
                .mixtureItemSeq(dur.getMixtureItemSeq())
                .mixtureItemName(dur.getMixtureItemName())
                .prohbtContent(dur.getProhbtContent())
                .build();
    }
}
