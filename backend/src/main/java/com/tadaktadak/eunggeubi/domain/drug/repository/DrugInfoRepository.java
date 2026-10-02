package com.tadaktadak.eunggeubi.domain.drug.repository;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugSearchSummary;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchSummary;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DrugInfoRepository extends JpaRepository<DrugInfo, String> {

    // 약품명 검색(페이지네이션). 낱알식별+e약은요+의약품 제품 허가정보가 모두 적재된 DB(27,000여
    // 건, 전문의약품 포함) 전체를 대상으로 하므로 이 결과만으로 검색/상세조회가 완결된다.
    // efficacy는 200자로 잘라 미리보기만 받고 useInfo/caution은 아예 안 받는다(DrugSearchSummary
    // 주석 참고 — 무거운 LONGTEXT 컬럼을 목록 조회에서 빼서 속도를 확보).
    @Query(value = "SELECT d.itemSeq AS itemSeq, d.name AS name, d.shape AS shape, d.color AS color, " +
            "d.imprint AS imprint, d.drugType AS drugType, d.itemImage AS itemImage, " +
            "SUBSTRING(d.efficacy, 1, 200) AS efficacySnippet " +
            "FROM DrugInfo d WHERE d.name LIKE CONCAT('%', :name, '%') ORDER BY d.name ASC",
            countQuery = "SELECT COUNT(d) FROM DrugInfo d WHERE d.name LIKE CONCAT('%', :name, '%')")
    Page<DrugSearchSummary> findSummaryByNameContaining(@Param("name") String name, Pageable pageable);

    // 낱알 특징(모양/색상/각인) 검색. 조건은 전부 선택사항이라 null이면 그 조건은 건너뛴다.
    // "원형"처럼 흔한 모양 하나만 줘도 9,837건이 매칭돼서(efficacy 등 LONGTEXT 컬럼까지 같이
    // 읽어오면 극도로 느려짐 — 실측: fetch가 타임아웃될 정도) 필요한 필드만 받는 프로젝션을 쓴다.
    @Query("SELECT d.itemSeq AS itemSeq, d.name AS name, d.entpName AS entpName, d.shape AS shape, " +
            "d.color AS color, d.imprint AS imprint, d.drugType AS drugType, d.itemImage AS itemImage " +
            "FROM DrugInfo d WHERE " +
            "(:shape IS NULL OR d.shape = :shape) AND " +
            "(:color IS NULL OR d.color = :color) AND " +
            "(:imprint IS NULL OR d.imprint LIKE %:imprint%)")
    List<PillSearchSummary> searchByAppearance(
            @Param("shape") String shape,
            @Param("color") String color,
            @Param("imprint") String imprint
    );

}