package com.tadaktadak.eunggeubi.domain.ai_consultations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI가 특정 상담(consultation) 응답에 대해 생성한 체크리스트 문항들.
// items는 문항 텍스트를 줄바꿈(\n)으로 이어붙인 TEXT다 - HealthProfile.diseases의 콤마 구분 관례와 같은 이유로
// 별도 문항 테이블/JSON 없이 이렇게 저장한다. 문항 텍스트 자체에 콤마가 섞이는 경우가 있어 구분자만 줄바꿈으로 바꿨다.
@Getter
@Entity
@Table(name = "checklists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "symptom_keyword", length = 100)
    private String symptomKeyword;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String source;

    @Column(columnDefinition = "TEXT", nullable = false) // 줄바꿈(\n) 구분
    private String items;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Checklist(Long consultationId, String symptomKeyword, String title, String source,
                       List<String> items, ChecklistStatus status) {
        this.consultationId = consultationId;
        this.symptomKeyword = symptomKeyword;
        this.title = title;
        this.source = source;
        this.items = String.join("\n", items);
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public List<String> itemList() {
        // "".split("\n")은 [""](원소 1개)를 주기 때문에 빈 경우를 별도로 처리한다.
        // Arrays.asList는 크기 고정 리스트라 .add()/.remove()를 부르면 UnsupportedOperationException이
        // 난다 - 지금은 아무도 그렇게 안 쓰지만, 애초에 완전히 불변으로 만들어서 그 가능성 자체를 없앤다.
        return items.isBlank() ? List.of() : List.of(items.split("\n"));
    }

    public void markCompleted() {
        this.status = ChecklistStatus.COMPLETED;
    }
}
