package com.tadaktadak.eunggeubi.domain.drug.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drug_ingredient")
@Getter
@NoArgsConstructor
public class DrugIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 약물성분 ID (PK)

    @Column(name = "name", length = 100, nullable = false)
    private String name; // 약품 성분명

    // 약물 정보(DrugInfo)와의 다대일 연관관계 세팅
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_seq")
    private DrugInfo drugInfo; // 약품 코드 (FK)
}