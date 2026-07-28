package com.tadaktadak.eunggeubi.domain.drug.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drug_infos")
@Getter
@NoArgsConstructor
public class DrugInfo {

    @Id
    @Column(name = "item_seq", length = 20)
    private String itemSeq; // 약품 코드 (PK)

    @Column(name = "name", nullable = false)
    private String name; // 약품명

    @Column(name = "shape", length = 50)
    private String shape; // 모양 - 낱알검색

    @Column(name = "color", length = 50)
    private String color; // 색상 - 낱알검색

    @Column(name = "imprint", length = 50)
    private String imprint; // 각인 - 낱알검색

    @Column(name = "efficacy", columnDefinition = "TEXT")
    private String efficacy; // 효능/효과

    @Column(name = "use_info", columnDefinition = "TEXT")
    private String useInfo; // 용법/용량

    @Column(name = "caution", columnDefinition = "TEXT")
    private String caution; // 주의사항

    @Column(name = "drug_type", length = 20)
    private String drugType; // 약품구분
}