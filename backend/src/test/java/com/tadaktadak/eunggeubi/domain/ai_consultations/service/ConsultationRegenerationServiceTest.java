package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawRegeneratedAnswer;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RegenerateResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ReferenceSource;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistResponseRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ReferenceSourceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

class ConsultationRegenerationServiceTest {

    private final AiConsultationRepository aiConsultationRepository = mock(AiConsultationRepository.class);
    private final ChecklistRepository checklistRepository = mock(ChecklistRepository.class);
    private final ChecklistResponseRepository checklistResponseRepository = mock(ChecklistResponseRepository.class);
    private final ReferenceSourceRepository referenceSourceRepository = mock(ReferenceSourceRepository.class);
    private final RagRetrievalService ragRetrievalService = mock(RagRetrievalService.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final AtomicLong idSequence = new AtomicLong(1000);

    private final ConsultationRegenerationService service = new ConsultationRegenerationService(
            aiConsultationRepository, checklistRepository, checklistResponseRepository,
            referenceSourceRepository, ragRetrievalService, chatClient);

    @Test
    void 본인_상담이_아니면_예외() {
        AiConsultation aiMessage = baseAiMessage(50L, "다른guest");
        when(aiConsultationRepository.findById(50L)).thenReturn(Optional.of(aiMessage));

        assertThatThrownBy(() -> service.regenerate(50L, null, "내guest"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 검색결과가_없으면_fallback_문구를_반환한다() {
        AiConsultation aiMessage = baseAiMessage(51L, "g1");
        when(aiConsultationRepository.findById(51L)).thenReturn(Optional.of(aiMessage));
        stubSave();
        when(checklistRepository.findTopByConsultationIdOrderByCreatedAtDesc(51L)).thenReturn(Optional.empty());
        when(ragRetrievalService.retrieve(anyString())).thenReturn(List.of());

        RegenerateResponse response = service.regenerate(51L, null, "g1");

        assertThat(response.message()).contains("제공된 정보로는");
        assertThat(response.isDiagnosis()).isFalse();
        assertThat(response.sources()).isEmpty();
    }

    @Test
    void 진단형_표현이_감지되면_폴백문구로_통째로_교체된다() {
        AiConsultation aiMessage = baseAiMessage(52L, "g1");
        when(aiConsultationRepository.findById(52L)).thenReturn(Optional.of(aiMessage));
        stubSave();
        when(checklistRepository.findTopByConsultationIdOrderByCreatedAtDesc(52L)).thenReturn(Optional.empty());

        Document doc = new Document("두통 관련 안내", Map.of(
                "disease", "두통", "section", "치료", "source", "질병관리청 국가건강정보포털", "cntntsSn", "1234"));
        when(ragRetrievalService.retrieve(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (두통) 두통 관련 안내");

        stubChatClientEntity(new RawRegeneratedAnswer("이 증상은 편두통입니다.", List.of(1)));

        RegenerateResponse response = service.regenerate(52L, null, "g1");

        assertThat(response.message()).doesNotContain("편두통입니다");
        assertThat(response.isDiagnosis()).isFalse();
        assertThat(response.sources()).isEmpty(); // 폴백이라 출처도 안 붙인다
    }

    @Test
    void 정상_케이스면_인용된_출처만_포함하고_존재하지_않는_번호는_걸러진다() {
        AiConsultation aiMessage = baseAiMessage(53L, "g1");
        when(aiConsultationRepository.findById(53L)).thenReturn(Optional.of(aiMessage));
        stubSave();
        when(checklistRepository.findTopByConsultationIdOrderByCreatedAtDesc(53L)).thenReturn(Optional.empty());

        Document doc = new Document("두통 관리 방법", Map.of(
                "disease", "두통", "section", "관리", "source", "질병관리청 국가건강정보포털", "cntntsSn", "1234"));
        when(ragRetrievalService.retrieve(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (두통) 두통 관리 방법");

        // 5는 존재하지 않는 번호(환각) - 걸러져야 한다
        stubChatClientEntity(new RawRegeneratedAnswer("충분한 휴식과 수분 섭취가 도움이 됩니다.", List.of(1, 5)));

        RegenerateResponse response = service.regenerate(53L, null, "g1");

        assertThat(response.message()).isEqualTo("충분한 휴식과 수분 섭취가 도움이 됩니다.");
        assertThat(response.isDiagnosis()).isFalse();
        assertThat(response.sources()).hasSize(1);
        assertThat(response.disclaimer()).isNotBlank();
    }

    private AiConsultation baseAiMessage(long id, String guestCode) {
        AiConsultation userMessage = AiConsultation.builder()
                .sessionId("s1").sessionRoot(true).guestCode(guestCode)
                .senderType(SenderType.USER).content("두통이 심해요").regenerated(false)
                .build();
        ReflectionTestUtils.setField(userMessage, "id", id - 1);
        when(aiConsultationRepository.findById(id - 1)).thenReturn(Optional.of(userMessage));

        AiConsultation aiMessage = AiConsultation.builder()
                .sessionId("s1").sessionRoot(false).guestCode(guestCode)
                .symptomKeyword("두통")
                .senderType(SenderType.AI).content("1차 안내").regenerated(false)
                .parentId(id - 1)
                .build();
        ReflectionTestUtils.setField(aiMessage, "id", id);
        return aiMessage;
    }

    private void stubSave() {
        when(aiConsultationRepository.save(any(AiConsultation.class))).thenAnswer(inv -> {
            AiConsultation entity = inv.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", idSequence.getAndIncrement());
            return entity;
        });
        when(referenceSourceRepository.save(any(ReferenceSource.class))).thenAnswer(inv -> {
            ReferenceSource entity = inv.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", idSequence.getAndIncrement());
            return entity;
        });
    }

    private void stubChatClientEntity(RawRegeneratedAnswer value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawRegeneratedAnswer.class)).thenReturn(value);
    }
}
