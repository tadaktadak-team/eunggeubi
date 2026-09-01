package com.tadaktadak.eunggeubi.domain.ai_consultations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 체크리스트에 응답한 결과. selected_items도 Checklist.items와 같은 이유로 줄바꿈(\n) 구분 TEXT.
@Getter
@Entity
@Table(name = "checklist_responses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checklist_id", nullable = false)
    private Long checklistId;

    @Column(name = "user_id") // 게스트는 null
    private Long userId;

    @Column(name = "selected_items", columnDefinition = "TEXT", nullable = false) // 줄바꿈(\n) 구분
    private String selectedItems;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChecklistResponse(Long checklistId, Long userId, List<String> selectedItems) {
        this.checklistId = checklistId;
        this.userId = userId;
        this.selectedItems = String.join("\n", selectedItems);
        this.createdAt = LocalDateTime.now();
    }

    public List<String> selectedItemList() {
        // "".split("\n")은 [""](원소 1개)를 주기 때문에, 체크한 게 하나도 없는 경우를 별도로 처리한다.
        return selectedItems.isBlank() ? List.of() : Arrays.asList(selectedItems.split("\n"));
    }
}
