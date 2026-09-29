package com.tadaktadak.eunggeubi.domain.drug.repository;

import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DrugInfoRepository extends JpaRepository<DrugInfo, String> {

    // 약품명에 특정 키워드가 포함된 약들을 찾아오는 명령어
    List<DrugInfo> findByNameContaining(String name);

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