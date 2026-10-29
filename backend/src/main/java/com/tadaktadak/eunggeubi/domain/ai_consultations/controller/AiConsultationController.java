package com.tadaktadak.eunggeubi.domain.ai_consultations.controller;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ChecklistDto;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.GenerateChecklistRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RegenerateRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RegenerateResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.SubmitChecklistRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.SubmitChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.AiConsultationService;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.ChecklistService;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.ConsultationRegenerationService;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.GuestLimitExceededException;
import com.tadaktadak.eunggeubi.global.exception.ErrorResponse;
import com.tadaktadak.eunggeubi.global.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 이 컨트롤러의 전체 경로(/api/ai-consultations/**)는 SecurityConfig에서 permitAll이라 비로그인
// 사용자도 쓸 수 있다. userId는 로그인 상태면 채워지고, 게스트면 null로 들어온다
// (JwtAuthenticationFilter가 토큰이 없어도 필터체인은 통과시키기 때문 - 401을 던지지 않음).
@Slf4j
@RestController
@RequestMapping("/api/ai-consultations")
@RequiredArgsConstructor
public class AiConsultationController {

    // RAG 검색 + 재시도 3회까지 넉넉히. 넘기면 연결을 끊는다(상담 저장은 끝까지 진행된다).
    private static final long STREAM_TIMEOUT_MS = 180_000;

    private final AiConsultationService aiConsultationService;
    private final ChecklistService checklistService;
    private final ConsultationRegenerationService consultationRegenerationService;
    private final Executor consultStreamExecutor; // AsyncConfig - 빈 이름과 필드 이름으로 매칭된다

    @PostMapping
    public ResponseEntity<ConsultationResponse> consult(@AuthenticationPrincipal Long userId,
                                                          @Valid @RequestBody ConsultationRequest request,
                                                          HttpServletRequest httpRequest) {
        // RateLimitFilter가 신뢰 프록시 설정까지 반영해 뽑아둔 IP. 필터가 안 탔을 때만 remoteAddr로.
        String clientIp = Objects.requireNonNullElse(
                (String) httpRequest.getAttribute(RateLimitFilter.CLIENT_IP_ATTR), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(aiConsultationService.consult(request, userId, clientIp));
    }

    // consult와 같은 상담을 SSE로 보낸다. 이벤트: partial(지금까지 완성된 문장들, 출처 없음) 여러 번 →
    // done(consult 응답과 같은 ConsultationResponse) 한 번, 또는 실패하면 error({message, code}) 한 번.
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter consultStream(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ConsultationRequest request,
                                    HttpServletRequest httpRequest) {
        String clientIp = Objects.requireNonNullElse(
                (String) httpRequest.getAttribute(RateLimitFilter.CLIENT_IP_ATTR), httpRequest.getRemoteAddr());
        return stream(onPartial -> aiConsultationService.consult(request, userId, clientIp, onPartial::accept));
    }

    // regenerate와 같은 재생성을 SSE로 보낸다. 이벤트는 consultStream과 같고, partial 데이터만 지금까지
    // 끝난 문장들을 이은 글({message})이다. done은 regenerate 응답과 같은 RegenerateResponse.
    // 글을 문자열 그대로 보내면 JSON이 아닌 맨 텍스트로 나가고, 줄바꿈이 있으면 SSE 줄 구분까지 깨진다.
    @PostMapping(value = "/{id}/regenerate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter regenerateStream(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long id,
                                       @RequestBody(required = false) RegenerateRequest request) {
        String guestCode = request != null ? request.guestCode() : null;
        return stream(onPartial -> consultationRegenerationService.regenerate(id, userId, guestCode,
                message -> onPartial.accept(Map.of("message", message))));
    }

    // work를 별도 스레드에서 돌리며 partial을 바로바로 보내고, 끝나면 done, 실패하면 error 이벤트로 마친다.
    // 응답 상태는 이미 200으로 나간 뒤라 비회원 한도 초과도 error 이벤트의 code(GUEST_LIMIT)로 알린다.
    private SseEmitter stream(Function<Consumer<Object>, Object> work) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        consultStreamExecutor.execute(() -> {
            try {
                Object result = work.apply(partial -> sendQuietly(emitter, "partial", partial));
                emitter.send(SseEmitter.event().name("done").data(result));
                emitter.complete();
            } catch (GuestLimitExceededException e) {
                sendError(emitter, new ErrorResponse(e.getMessage(), "GUEST_LIMIT"));
            } catch (IllegalArgumentException e) {
                sendError(emitter, new ErrorResponse(e.getMessage(), null)); // 일반 요청이면 400으로 가던 안내
            } catch (Exception e) {
                log.warn("AI 상담 스트리밍 실패", e);
                sendError(emitter, new ErrorResponse("요청 중 오류가 발생했습니다.", null));
            }
        });
        return emitter;
    }

    // 중간에 앱을 닫아 연결이 끊겨도 상담은 끝까지 만들어 저장한다(일반 consult와 같게) - 보내기 실패는 무시.
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

    @PostMapping("/{id}/checklist")
    public ResponseEntity<ChecklistDto> generateChecklist(@AuthenticationPrincipal Long userId,
                                                            @PathVariable Long id,
                                                            @RequestBody(required = false) GenerateChecklistRequest request) {
        String guestCode = request != null ? request.guestCode() : null;
        return ResponseEntity.ok(checklistService.generate(id, userId, guestCode));
    }

    @PostMapping("/{id}/checklist/submit")
    public ResponseEntity<SubmitChecklistResponse> submitChecklist(@AuthenticationPrincipal Long userId,
                                                                     @PathVariable Long id,
                                                                     @Valid @RequestBody SubmitChecklistRequest request) {
        return ResponseEntity.ok(checklistService.submit(id, request, userId));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<RegenerateResponse> regenerate(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long id,
                                                           @RequestBody(required = false) RegenerateRequest request) {
        String guestCode = request != null ? request.guestCode() : null;
        return ResponseEntity.ok(consultationRegenerationService.regenerate(id, userId, guestCode));
    }
}
