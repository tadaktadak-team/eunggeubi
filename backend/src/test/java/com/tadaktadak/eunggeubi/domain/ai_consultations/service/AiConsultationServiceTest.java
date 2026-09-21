package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawAnswer;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ReferenceSource;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ReferenceSourceRepository;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

// VectorStore/ChatClient는 실제로 호출하지 않고 RagRetrievalService/ChatClient를 mock으로 대체한다.
// repository.save()는 실제 DB 없이, 넘어온 엔티티에 id만 채워서 그대로 돌려주도록 흉내낸다
// (JPA IDENTITY 채번을 mock으로 재현 - HealthInfoIndexingRunnerTest와 같은 스타일).
// 검색어 재작성 자체의 성공/실패 폴백 동작은 RagRetrievalServiceTest 책임이다 - 여기서는
// ragRetrievalService.rewriteForSearch가 돌려준 값을 AiConsultationService가 실제로 검색에 쓰는지만 본다.
class AiConsultationServiceTest {

    private final RagRetrievalService ragRetrievalService = mock(RagRetrievalService.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final AiConsultationRepository aiConsultationRepository = mock(AiConsultationRepository.class);
    private final ReferenceSourceRepository referenceSourceRepository = mock(ReferenceSourceRepository.class);
    private final HealthProfileRepository healthProfileRepository = mock(HealthProfileRepository.class);
    private final AtomicLong idSequence = new AtomicLong(1);

    private final AiConsultationService service = new AiConsultationService(
            ragRetrievalService, chatClient, aiConsultationRepository, referenceSourceRepository,
            healthProfileRepository);

    @Test
    void 검색결과가_없으면_저장은_하되_fallback_문구를_반환한다() {
        stubSave();
        stubRewrite("주식 투자로 돈 버는 방법 알려줘");
        when(ragRetrievalService.retrieveGrouped(anyString())).thenReturn(List.of());

        ConsultationResponse response = service.consult(
                new ConsultationRequest("주식 투자로 돈 버는 방법 알려줘", null, null), null);

        assertThat(response.answer()).hasSize(1); // fallback 문장 하나 (disclaimer는 이제 별도 필드)
        assertThat(response.answer().get(0).text()).contains("제공된 정보로는 답변드리기 어렵습니다");
        assertThat(response.disclaimer()).isEqualTo(ConsultationDisclaimer.TEXT);
        assertThat(response.sources()).isEmpty();
        assertThat(response.consultationId()).isNotNull();
        assertThat(response.sessionId()).isNotNull();
        assertThat(response.guestCode()).isNotNull(); // 게스트(userId=null) + 새 세션이라 새로 발급됨

        // USER 메시지 1건 + AI(fallback) 메시지 1건, 총 2번 저장돼야 한다
        verify(aiConsultationRepository, times(2)).save(any());
    }

    @Test
    void 정상_생성시_존재하지_않는_인용번호는_걸러지고_인용된_출처만_반환한다() {
        stubSave();
        stubRewrite("화상 응급처치 어떻게 하나요");
        Document doc = new Document("화상 부위를 찬물로 식힙니다.", Map.of(
                "disease", "화상", "section", "치료", "source", "질병관리청 국가건강정보포털", "cntntsSn", "6584"));
        when(ragRetrievalService.retrieveGrouped(anyString())).thenReturn(List.of(List.of(doc)));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (화상) 화상 부위를 찬물로 식힙니다.");

        RawAnswer raw = new RawAnswer(List.of(
                new RawAnswer.RawSegment("화상 부위를 찬물로 20분 이상 식힙니다.", List.of(1)),
                // 9는 실제 문서 범위(1개)를 벗어난 환각 인용 - 걸러져야 한다
                new RawAnswer.RawSegment("병원 진료가 필요할 수 있습니다.", List.of(1, 9))));
        stubChatClientEntity(RawAnswer.class, raw);

        ConsultationResponse response = service.consult(
                new ConsultationRequest("화상 응급처치 어떻게 하나요", null, "existing-guest-code"), null);

        assertThat(response.answer()).hasSize(2); // 문장 2개 (disclaimer는 이제 별도 필드)
        assertThat(response.answer().get(1).sourceIndexes()).containsExactly(1); // 9는 필터링됨
        assertThat(response.disclaimer()).isEqualTo(ConsultationDisclaimer.TEXT);
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).disease()).isEqualTo("화상");
        // 이미 guestCode를 들고 있는 요청이라 새로 발급하지 않는다
        assertThat(response.guestCode()).isNull();

        verify(referenceSourceRepository, times(1)).save(any(ReferenceSource.class));
    }

    @Test
    void 개별_segment의_null_필드는_방어적으로_처리한다() {
        stubSave();
        stubRewrite("화상 응급처치 어떻게 하나요");
        Document doc = new Document("화상 부위를 찬물로 식힙니다.", Map.of(
                "disease", "화상", "section", "치료", "source", "질병관리청 국가건강정보포털", "cntntsSn", "6584"));
        when(ragRetrievalService.retrieveGrouped(anyString())).thenReturn(List.of(List.of(doc)));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (화상) 화상 부위를 찬물로 식힙니다.");

        RawAnswer raw = new RawAnswer(List.of(
                // text가 null인 segment - 보여줄 내용이 없으니 걸러져야 한다
                new RawAnswer.RawSegment(null, List.of(1)),
                // sourceIndexes가 null인 segment - NPE 없이 인용 없는 문장으로 처리돼야 한다
                new RawAnswer.RawSegment("병원 진료가 필요할 수 있습니다.", null)));
        stubChatClientEntity(RawAnswer.class, raw);

        ConsultationResponse response = service.consult(
                new ConsultationRequest("화상 응급처치 어떻게 하나요", null, "existing-guest-code"), null);

        assertThat(response.answer()).hasSize(1); // null text segment는 걸러짐
        assertThat(response.answer().get(0).text()).isEqualTo("병원 진료가 필요할 수 있습니다.");
        assertThat(response.answer().get(0).sourceIndexes()).isEmpty(); // null -> 빈 리스트
        assertThat(response.sources()).isEmpty(); // 인용된 게 없으니 출처도 없음
    }

    @Test
    void 체크리스트용_symptomKeyword는_실제로_인용된_문서에서_뽑는다() {
        stubSave();
        stubRewrite("두통이 있어요");
        // 검색 1등(고혈압)은 실제 답변에서 한 번도 인용 안 되고, 2등(두통)만 인용되는 상황.
        Document topButUncited = new Document("고혈압 관련 내용", Map.of(
                "disease", "고혈압", "section", "정의", "source", "질병관리청 국가건강정보포털", "cntntsSn", "1111"));
        Document actuallyCited = new Document("두통 관련 내용", Map.of(
                "disease", "두통", "section", "정의", "source", "질병관리청 국가건강정보포털", "cntntsSn", "2222"));
        when(ragRetrievalService.retrieveGrouped(anyString())).thenReturn(List.of(List.of(topButUncited, actuallyCited)));
        when(ragRetrievalService.buildContext(List.of(topButUncited, actuallyCited))).thenReturn("...");

        // 2번(두통)만 인용
        stubChatClientEntity(RawAnswer.class, new RawAnswer(
                List.of(new RawAnswer.RawSegment("두통에는 충분한 휴식이 도움이 됩니다.", List.of(2)))));

        service.consult(new ConsultationRequest("두통이 있어요", null, "g1"), null);

        ArgumentCaptor<AiConsultation> captor = ArgumentCaptor.forClass(AiConsultation.class);
        verify(aiConsultationRepository, times(2)).save(captor.capture());
        AiConsultation aiMessage = captor.getAllValues().get(1); // 1번째는 USER 저장, 2번째가 AI 저장
        // 검색 1등인 "고혈압"이 아니라, 실제로 인용된 "두통"이어야 한다.
        assertThat(aiMessage.getSymptomKeyword()).isEqualTo("두통");
    }

    @Test
    void 재작성된_검색어로_검색한다() {
        stubSave();
        when(ragRetrievalService.rewriteForSearch("발이 부어요")).thenReturn("부종 다리 발목 붓기");
        Document doc = new Document("부종 관련 내용", Map.of(
                "disease", "부종", "section", "정의", "source", "질병관리청 국가건강정보포털", "cntntsSn", "6544"));
        when(ragRetrievalService.retrieveGrouped(anyString())).thenReturn(List.of(List.of(doc)));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("...");
        stubChatClientEntity(RawAnswer.class, new RawAnswer(
                List.of(new RawAnswer.RawSegment("부종은 다리에 흔히 생깁니다.", List.of(1)))));

        service.consult(new ConsultationRequest("발이 부어요", null, "g1"), null);

        ArgumentCaptor<String> searchQueryCaptor = ArgumentCaptor.forClass(String.class);
        verify(ragRetrievalService).retrieveGrouped(searchQueryCaptor.capture());
        // 원래 질문("발이 부어요") 그대로가 아니라, RagRetrievalService가 재작성해 돌려준 검색어로 검색해야 한다.
        assertThat(searchQueryCaptor.getValue()).isEqualTo("부종 다리 발목 붓기");
    }

    private void stubRewrite(String query) {
        when(ragRetrievalService.rewriteForSearch(query)).thenReturn(query);
    }

    private void stubSave() {
        when(aiConsultationRepository.save(any(AiConsultation.class))).thenAnswer(inv -> {
            AiConsultation entity = inv.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", idSequence.getAndIncrement());
            return entity;
        });
        when(referenceSourceRepository.save(any(ReferenceSource.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private <T> void stubChatClientEntity(Class<T> type, T value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(type)).thenReturn(value);
    }
}
