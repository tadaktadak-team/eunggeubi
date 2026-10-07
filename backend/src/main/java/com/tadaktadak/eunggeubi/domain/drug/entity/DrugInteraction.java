package com.tadaktadak.eunggeubi.domain.drug.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// DUR 병용금기 데이터를 담는 테이블. DrugInfo와 다르게 itemSeq 하나로 유일하게 정해지는
// 게 아니라 "약 A - 약 B" 쌍이 데이터라서, PK는 그냥 auto-increment로 두고 재적재할 땐
// 전체를 지운 뒤 새로 넣는 방식(DurInteractionIndexingRunner)을 쓴다.
@Entity
@Table(
        name = "drug_interactions",
        indexes = {
                @Index(name = "idx_drug_interactions_item_seq", columnList = "item_seq"),
                @Index(name = "idx_drug_interactions_mixture_item_seq", columnList = "mixture_item_seq")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_seq", length = 20, nullable = false)
    private String itemSeq;          // 기준 약

    // 복합제 약품명이 255자를 넘는 경우가 실제로 있어서(적재 중 Data truncation 에러로 확인함) TEXT로 둔다.
    @Column(name = "item_name", columnDefinition = "TEXT")
    private String itemName;

    @Column(name = "mixture_item_seq", length = 20, nullable = false)
    private String mixtureItemSeq;   // 병용금기 상대 약

    @Column(name = "mixture_item_name", columnDefinition = "TEXT")
    private String mixtureItemName;

    @Column(name = "prohbt_content", columnDefinition = "TEXT")
    private String prohbtContent;    // 금기 사유
}
