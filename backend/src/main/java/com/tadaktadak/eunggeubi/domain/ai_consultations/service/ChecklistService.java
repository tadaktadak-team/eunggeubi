package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI 응답(ai_consultations, sender_type=AI)에 대해 체크리스트를 생성하고, 사용자의 체크 응답을 저장한다.
// "선택된 항목(selected_items)"은 오래된 프론트 목업의 체크박스와 같은 의미다 - 문항은 "~있나요?" 형태의
// 질문이고, 사용자가 체크(=예)한 항목만 selected_items에 저장된다. 체크 안 한 항목은 "아니오"로 간주하고
// 별도 저장하지 않는다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistService {

    // LLM이 문항을 하나도 못 만들거나 스키마를 벗어난 응답을 줬을 때 쓰는 최소한의 기본 체크리스트.
    private static final List<String> DEFAULT_ITEMS = List.of(
            "증상이 3일 이상 지속되나요?",
            "다른 통증이나 증상이 동반되나요?",
            "갑자기 심해졌나요?"
    );

    private static final String SOURCE_NAME = "질병관리청 국가건강정보포털";

    private final AiConsultationRepository aiConsultationRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistResponseRepository checklistResponseRepository;
    private final ChatClient chatClient;

    @Transactional
    public ChecklistDto generate(Long consultationId, Long userId, String guestCode) {
        AiConsultation aiMessage = getOwnedAiMessage(consultationId, userId, guestCode);
        String symptomText = resolveSymptomText(aiMessage);

        // 원본 증상 텍스트만 주면 매 요청이 사실상 동일한 입력이라 LLM이 항상 같은(뻔한) 문항을
        // 반복 생성한다. AI의 1차 분석 답변과 그 답변이 식별한 질병 키워드를 함께 줘서, 그 케이스에서
        // 실제로 중요하다고 짚은 감별 포인트를 문항에 반영하게 한다.
        String symptomKeyword = aiMessage.getSymptomKeyword() != null ? aiMessage.getSymptomKeyword() : "특정되지 않음";
        String userPrompt = """
                [증상]
                %s

                [분석된 질병/증상]
                %s

                [1차 분석]
                %s""".formatted(symptomText, symptomKeyword, aiMessage.getContent());

        List<String> items = generateItemsWithRetry(consultationId, userPrompt);

        String title = "%s 관련 확인사항".formatted(
                aiMessage.getSymptomKeyword() != null ? aiMessage.getSymptomKeyword() : "증상");

        Checklist checklist = checklistRepository.save(Checklist.builder()
                .consultationId(consultationId)
                .symptomKeyword(aiMessage.getSymptomKeyword())
                .title(title)
                .source(SOURCE_NAME)
                .items(items)
                .status(ChecklistStatus.PROPOSED)
                .build());

        return new ChecklistDto(checklist.getId(), checklist.getTitle(), checklist.itemList(),
                checklist.getStatus().name());
    }

    @Transactional
    public SubmitChecklistResponse submit(Long consultationId, SubmitChecklistRequest request, Long userId) {
        getOwnedAiMessage(consultationId, userId, request.guestCode());

        Checklist checklist = checklistRepository.findTopByConsultationIdOrderByCreatedAtDesc(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("생성된 체크리스트가 없습니다."));

        ChecklistResponse response = checklistResponseRepository.save(ChecklistResponse.builder()
                .checklistId(checklist.getId())
                .userId(userId)
                .selectedItems(request.selectedItems())
                .build());

        checklist.markCompleted();

        return new SubmitChecklistResponse(response.getId(), checklist.getStatus().name());
    }

    // consultationId는 항상 AI 메시지(1차 답변 또는 재생성 응답)의 id를 가리켜야 한다 - 체크리스트는
    // "AI가 준 답변"에 대해 만드는 것이지 사용자 메시지에 대해 만드는 게 아니다.
    private AiConsultation getOwnedAiMessage(Long consultationId, Long userId, String guestCode) {
        AiConsultation consultation = aiConsultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("상담 내역을 찾을 수 없습니다."));
        if (!consultation.isOwnedBy(userId, guestCode)) {
            throw new IllegalArgumentException("본인 상담 세션이 아닙니다.");
        }
        if (consultation.getSenderType() != SenderType.AI) {
            throw new IllegalArgumentException("AI 응답에 대해서만 체크리스트를 만들 수 있습니다.");
        }
        return consultation;
    }

    // LLM이 요청한 JSON 스키마를 안 지키고 다른 형태(빈 응답, 산문 등)로 답하는 경우가 실제로 잦다 -
    // 특히 체크리스트처럼 짧은 프롬프트에서 그렇다. 한 번 실패했다고 바로 기본값으로 넘어가면 사실상
    // 항상 기본값만 나오게 되므로, 몇 번 더 시도해보고 그래도 안 되면 그때 기본값으로 폴백한다.
    private static final int MAX_ATTEMPTS = 3;

    private List<String> generateItemsWithRetry(Long consultationId, String userPrompt) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RawChecklist raw = chatClient.prompt()
                    .system(AiConsultationPrompts.CHECKLIST_PROMPT)
                    .user(userPrompt)
                    .call()
                    .entity(RawChecklist.class);

            if (raw != null && raw.questions() != null && !raw.questions().isEmpty()) {
                return raw.questions();
            }
            log.warn("체크리스트 LLM 구조화 출력이 예상 스키마를 벗어났습니다 (시도 {}/{}). consultationId={}, raw={}",
                    attempt, MAX_ATTEMPTS, consultationId, raw);
        }
        return DEFAULT_ITEMS;
    }

    // 체크리스트는 AI의 답변 내용이 아니라 사용자가 처음 입력한 증상 텍스트를 근거로 만들어야 하므로,
    // parent_id(원본 사용자 메시지)를 따라가서 원문을 가져온다.
    private String resolveSymptomText(AiConsultation aiMessage) {
        if (aiMessage.getParentId() == null) {
            return aiMessage.getContent();
        }
        return aiConsultationRepository.findById(aiMessage.getParentId())
                .map(AiConsultation::getContent)
                .orElse(aiMessage.getContent());
    }
}
