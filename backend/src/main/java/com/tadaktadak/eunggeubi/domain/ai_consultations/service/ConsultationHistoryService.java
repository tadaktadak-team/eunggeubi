package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationDetailResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationSummaryResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.Checklist;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistResponseRepository;
import com.tadaktadak.eunggeubi.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//마이페이지 상담 이력 조회
@Service
@RequiredArgsConstructor
public class ConsultationHistoryService {

    private final AiConsultationRepository aiConsultationRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistResponseRepository checklistResponseRepository;

    @Transactional(readOnly = true)
    public List<ConsultationSummaryResponse> getMyConsultations(Long userId) {
        return aiConsultationRepository.findByUserIdAndSessionRootTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(ConsultationSummaryResponse::from)
                .toList();
    }

    // 비회원 기록(guestCode, userId 없음)을 이 계정으로 옮긴다. 이미 다른 계정으로 옮겨진 건 안 건드린다.
    @Transactional
    public int claimGuestConsultations(Long userId, String guestCode) {
        return aiConsultationRepository.claimGuestConsultations(userId, guestCode);
    }

    @Transactional(readOnly = true)
    public ConsultationDetailResponse getMyConsultationDetail(Long userId, String sessionId) {
        List<AiConsultation> messages =
                aiConsultationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // 없는 상담과 남의 상담을 같은 응답(404)으로 돌려준다(보호자 조회와 같은 이유: 존재 여부를 알 수 없게)
        if (messages.isEmpty() || !messages.get(0).isOwnedBy(userId, null)) {
            throw new ResourceNotFoundException("상담 내역을 찾을 수 없습니다.");
        }

        Map<Long, List<String>> checkedItems = loadCheckedItems(messages);

        List<ConsultationDetailResponse.Message> dtos = messages.stream()
                .map(m -> new ConsultationDetailResponse.Message(
                        m.getId(),
                        m.getSenderType().name(),
                        m.getContent(),
                        m.isRegenerated(),
                        m.getCreatedAt(),
                        checkedItems.getOrDefault(m.getId(), List.of()),
                        checkedItems.containsKey(m.getId()),
                        m.getSenderType() == SenderType.AI ? ConsultationDisclaimer.TEXT : null))
                .toList();

        return new ConsultationDetailResponse(sessionId, dtos);
    }

    //메세지마다 조회 시 N+1 문제->세션 전체 체크리스트와 응답을 가져와서 합치기
    private Map<Long, List<String>> loadCheckedItems(List<AiConsultation> messages) {
        List<Long> aiMessageIds = messages.stream()
                .filter(m -> m.getSenderType() == SenderType.AI)
                .map(AiConsultation::getId)
                .toList();
        if (aiMessageIds.isEmpty()) {
            return Map.of();
        }
        
        Map<Long, Checklist> latestByConsultation = checklistRepository.findByConsultationIdIn(aiMessageIds).stream()
                .collect(Collectors.toMap(
                        Checklist::getConsultationId,
                        c -> c,
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b));
        if (latestByConsultation.isEmpty()) {
            return Map.of();
        }

        Map<Long, ChecklistResponse> latestResponses =
                checklistResponseRepository.findByChecklistIdIn(
                                latestByConsultation.values().stream().map(Checklist::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                ChecklistResponse::getChecklistId,
                                r -> r,
                                (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b));

        Map<Long, List<String>> result = new HashMap<>();
        latestByConsultation.forEach((consultationId, checklist) -> {
            ChecklistResponse response = latestResponses.get(checklist.getId());
            if (response == null) {
                return;
            }
            result.put(consultationId, response.selectedItemList());
        });
        return result;
    }
}