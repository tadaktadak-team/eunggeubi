package com.tadaktadak.eunggeubi.domain.drug.repository;

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

    // 약품명 검색(페이지네이션). 낱알식별+e약은요가 모두 적재된 DB(27,000여 건, 전문의약품
    // 포함) 전체를 대상으로 하므로 이 결과만으로 검색/상세조회가 완결된다.
    Page<DrugInfo> findByNameContainingOrderByNameAsc(String name, Pageable pageable);

    // 낱알 특징(모양/색상/각인) 검색. 조건은 전부 선택사항이라 null이면 그 조건은 건너뛴다.
    @Query("SELECT d FROM DrugInfo d WHERE " +
            "(:shape IS NULL OR d.shape = :shape) AND " +
            "(:color IS NULL OR d.color = :color) AND " +
            "(:imprint IS NULL OR d.imprint LIKE %:imprint%)")
    List<DrugInfo> searchByAppearance(
            @Param("shape") String shape,
            @Param("color") String color,
            @Param("imprint") String imprint
    );

}