package com.tadaktadak.eunggeubi.domain.drug.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drug_infos", indexes = {
        @Index(name = "idx_drug_infos_name", columnList = "name"),
        @Index(name = "idx_drug_infos_shape_color", columnList = "shape, color"),
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
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

    // TEXT(65,535바이트)로는 의약품 제품 허가정보 API의 주의사항 원문이 넘칠 수 있어 LONGTEXT로 둔다.
    // efficacy/useInfo/caution/cautionSections 중 전문의약품(의약품 제품 허가정보 API 소스) 분량은
    // 상세화면에서 현재 노출하지 않는다 — 임상시험 수치·금기 목록 등 규제 문서 원문이라 일반의약품
    // (e약은요, 짧은 소비자용 문구)과 결이 너무 달라서, 전문의약품은 외형정보 + 식약처 원문 링크로만
    // 안내하기로 함(DrugDetailScreen.isPrescription 참고). 데이터 자체는 재사용 가능성(AI상담 RAG
    // 등)이 있어 지우지 않고 그대로 보관한다.
    @Column(name = "efficacy", columnDefinition = "LONGTEXT")
    private String efficacy; // 효능/효과

    @Column(name = "use_info", columnDefinition = "LONGTEXT")
    private String useInfo; // 용법/용량

    @Column(name = "caution", columnDefinition = "LONGTEXT")
    private String caution; // 주의사항 (e약은요 등 평문 소스)

    // 의약품 제품 허가정보 API 소스 전용. "1. 다음 환자에는 투여하지 말 것" 같은 항목 단위로 쪼갠
    // [{title, body}, ...] JSON 배열. caution(평문)과 별개 컬럼으로 둬서, 전문의약품처럼 caution이
    // 비어있던 약만 이 필드로 채우고 기존 e약은요 평문 데이터는 안 건드린다.
    @Column(name = "caution_sections", columnDefinition = "LONGTEXT")
    private String cautionSections;

    @Column(name = "drug_type", length = 20)
    private String drugType; // 약품구분

    // 의약품 제품 허가정보 API의 CANCEL_NAME 원문("정상", "취하", "유효기간만료", "행정(취소)" 등).
    // null은 아직 확인 못 한 행(허가정보 배치가 안 거쳤거나 해당 페이지가 실패한 경우)이라 "정상"으로
    // 취급해 숨기지 않는다. 숨기는 대신 화면에서 상태 배지로 표시한다(집에 남아있는 예전 약을
    // 검색하는 경우가 있어서, 검색에서 지우면 오히려 "없는 약"으로 보여 곤란함).
    @Column(name = "cancel_name", length = 30)
    private String cancelName;

    @Column(name = "entp_name", length = 100)
    private String entpName; // 제조/수입 업체명 - 낱알검색

    @Column(name = "item_image", length = 500)
    private String itemImage; // 알약 이미지 URL
}