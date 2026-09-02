package com.tadaktadak.eunggeubi.domain.drug.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_histories")
@Getter
@NoArgsConstructor
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 검색이력 ID (PK)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 검색시간

    // 약물 정보(DrugInfo)와의 다대일 연관관계 세팅
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_seq")
    private DrugInfo drugInfo; // 검색한 약품 정보

    @Column(name = "user_id")
    private Long userId; // 회원 ID (FK)
}