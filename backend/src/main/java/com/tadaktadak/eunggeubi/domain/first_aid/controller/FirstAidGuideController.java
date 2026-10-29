package com.tadaktadak.eunggeubi.domain.first_aid.controller;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidGuideResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidSearchResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.SearchFirstAidGuideRequest;
import com.tadaktadak.eunggeubi.domain.first_aid.service.FirstAidGuideService;
import com.tadaktadak.eunggeubi.global.exception.ErrorResponse;
import java.io.IOException;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// SecurityConfig에서 permitAll - 응급처치 가이드는 로그인 여부와 상관없이 누구나 찾아볼 수 있어야 한다.
@Slf4j
@RestController
@RequestMapping("/api/first-aid/guides")
@RequiredArgsConstructor
public class FirstAidGuideController {

    // 검색 + 생성 재시도 3회 + 순서 판단까지 넉넉히(AiConsultationController와 같은 값).
    private static final long STREAM_TIMEOUT_MS = 180_000;

    private final FirstAidGuideService firstAidGuideService;
    private final Executor consultStreamExecutor; // AsyncConfig - 빈 이름과 필드 이름으로 매칭된다

    // 고를 수 있는 응급처치 주제 전체(first-aid-topics.txt). 경로 변수보다 우선 매칭되어 "/{situation}"과 안 겹친다.
    @GetMapping("/topics")
    public FirstAidSearchResponse topics() {
        return firstAidGuideService.listTopics();
    }

    // case는 세부 상황이 있는 주제(예: 동물·벌레 물림)에서 사용자가 고른 것 - 없으면 고를 목록(cases)이 온다.
    @GetMapping("/{situation}")
    public FirstAidGuideResponse getBySituation(@PathVariable String situation,
                                                @RequestParam(name = "case", required = false) String caseName) {
        return firstAidGuideService.getBySituation(situation, caseName);
    }

    // getBySituation과 같은 조회를 SSE로 보낸다(AiConsultationController.consultStream과 같은 이벤트 규약).
    // partial(지금까지 완성된 step들의 FirstAidGuideResponse, ordered=false) 여러 번 → done(최종 응답) 한 번,
    // 실패하면 error({message, code}) 한 번. 캐시된 가이드나 세부 상황 고르기는 partial 없이 바로 done이다.
    // 프론트 스트림 클라이언트(api.postStream)가 POST만 받아서 POST로 연다.
    @PostMapping(value = "/{situation}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getBySituationStream(@PathVariable String situation,
                                           @RequestParam(name = "case", required = false) String caseName) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        consultStreamExecutor.execute(() -> {
            try {
                FirstAidGuideResponse result = firstAidGuideService.getBySituation(situation, caseName,
                        partial -> sendQuietly(emitter, "partial", partial));
                emitter.send(SseEmitter.event().name("done").data(result));
                emitter.complete();
            } catch (IllegalArgumentException e) {
                sendError(emitter, new ErrorResponse(e.getMessage(), null)); // 일반 요청이면 400으로 가던 안내
            } catch (Exception e) {
                log.warn("응급처치 가이드 스트리밍 실패. situation=\"{}\"", situation, e);
                sendError(emitter, new ErrorResponse("요청 중 오류가 발생했습니다.", null));
            }
        });
        return emitter;
    }

    // 연결이 끊겨도 가이드는 끝까지 만들어 캐싱한다 - 보내기 실패는 무시.
    private static void sendQuietly(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException ignored) {
            // 클라이언트 연결 끊김
        }
    }

    private static void sendError(SseEmitter emitter, ErrorResponse error) {
        sendQuietly(emitter, "error", error);
        emitter.complete();
    }

    // 매칭되는 상황이 없어도 404 대신 200 + 빈 candidates로 내려준다 - 프론트가 "못 찾음" 안내를 띄운다.
    @PostMapping("/search")
    public FirstAidSearchResponse search(@RequestBody SearchFirstAidGuideRequest request) {
        return firstAidGuideService.searchCandidates(request.text());
    }
}
