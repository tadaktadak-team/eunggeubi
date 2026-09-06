package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ChecklistDto;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawChecklist;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.SubmitChecklistRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.SubmitChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.Checklist;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistStatus;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistResponseRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

class ChecklistServiceTest {

    private final AiConsultationRepository aiConsultationRepository = mock(AiConsultationRepository.class);
    private final ChecklistRepository checklistRepository = mock(ChecklistRepository.class);
    private final ChecklistResponseRepository checklistResponseRepository = mock(ChecklistResponseRepository.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final AtomicLong idSequence = new AtomicLong(1);

    private final ChecklistService service = new ChecklistService(
            aiConsultationRepository, checklistRepository, checklistResponseRepository, chatClient);

    @Test
    void 본인_상담이_아니면_예외() {
        AiConsultation aiMessage = aiMessage(10L, "다른사람guest", "두통");
        when(aiConsultationRepository.findById(10L)).thenReturn(Optional.of(aiMessage));

        assertThatThrownBy(() -> service.generate(10L, null, "내guest코드"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void USER_메시지에는_체크리스트를_못_만든다() {
        AiConsultation userMessage = AiConsultation.builder()
                .sessionId("s1").sessionRoot(true).guestCode("g1")
                .senderType(SenderType.USER).content("두통이 심해요").regenerated(false)
                .build();
        ReflectionTestUtils.setField(userMessage, "id", 11L);
        when(aiConsultationRepository.findById(11L)).thenReturn(Optional.of(userMessage));

        assertThatThrownBy(() -> service.generate(11L, null, "g1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정상_생성시_LLM_문항을_저장한다() {
        AiConsultation aiMessage = aiMessage(20L, "g1", "두통");
        when(aiConsultationRepository.findById(20L)).thenReturn(Optional.of(aiMessage));
        stubChecklistSave();
        stubChatClientEntity(new RawChecklist(List.of("3일 이상 지속되나요?", "구토가 동반되나요?")));

        ChecklistDto dto = service.generate(20L, null, "g1");

        assertThat(dto.items()).containsExactly("3일 이상 지속되나요?", "구토가 동반되나요?");
        assertThat(dto.status()).isEqualTo("PROPOSED");
        assertThat(dto.title()).contains("두통");
    }

    @Test
    void LLM이_빈_문항을_주면_기본_체크리스트로_대체한다() {
        AiConsultation aiMessage = aiMessage(21L, "g1", "두통");
        when(aiConsultationRepository.findById(21L)).thenReturn(Optional.of(aiMessage));
        stubChecklistSave();
        stubChatClientEntity(new RawChecklist(List.of()));

        ChecklistDto dto = service.generate(21L, null, "g1");

        assertThat(dto.items()).isNotEmpty();
    }

    @Test
    void submit하면_체크리스트_상태가_COMPLETED로_바뀐다() {
        AiConsultation aiMessage = aiMessage(30L, "g1", "두통");
        when(aiConsultationRepository.findById(30L)).thenReturn(Optional.of(aiMessage));

        Checklist checklist = Checklist.builder()
                .consultationId(30L).symptomKeyword("두통").title("두통 관련 확인사항")
                .source("질병관리청 국가건강정보포털")
                .items(List.of("3일 이상 지속되나요?"))
                .status(ChecklistStatus.PROPOSED)
                .build();
        ReflectionTestUtils.setField(checklist, "id", 100L);
        when(checklistRepository.findTopByConsultationIdOrderByCreatedAtDesc(30L))
                .thenReturn(Optional.of(checklist));
        when(checklistResponseRepository.save(any(ChecklistResponse.class))).thenAnswer(inv -> {
            ChecklistResponse entity = inv.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 200L);
            return entity;
        });

        SubmitChecklistResponse response = service.submit(
                30L, new SubmitChecklistRequest(List.of("3일 이상 지속되나요?"), "g1"), null);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(checklist.getStatus()).isEqualTo(ChecklistStatus.COMPLETED);
    }

    private AiConsultation aiMessage(long id, String guestCode, String symptomKeyword) {
        AiConsultation userMessage = AiConsultation.builder()
                .sessionId("s1").sessionRoot(true).guestCode(guestCode)
                .senderType(SenderType.USER).content(symptomKeyword + "이 심해요").regenerated(false)
                .build();
        ReflectionTestUtils.setField(userMessage, "id", id - 1);
        when(aiConsultationRepository.findById(id - 1)).thenReturn(Optional.of(userMessage));

        AiConsultation aiMessage = AiConsultation.builder()
                .sessionId("s1").sessionRoot(false).guestCode(guestCode)
                .symptomKeyword(symptomKeyword)
                .senderType(SenderType.AI).content("참고 정보 안내").regenerated(false)
                .parentId(id - 1)
                .build();
        ReflectionTestUtils.setField(aiMessage, "id", id);
        return aiMessage;
    }

    private void stubChecklistSave() {
        when(checklistRepository.save(any(Checklist.class))).thenAnswer(inv -> {
            Checklist entity = inv.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", idSequence.getAndIncrement());
            return entity;
        });
    }

    private void stubChatClientEntity(RawChecklist value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawChecklist.class)).thenReturn(value);
    }
}
