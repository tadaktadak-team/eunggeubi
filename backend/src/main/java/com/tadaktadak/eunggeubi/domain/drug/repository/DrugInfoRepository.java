package com.tadaktadak.eunggeubi.domain.drug.repository;

import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DrugInfoRepository extends JpaRepository<DrugInfo, String> {

    // 약품명에 특정 키워드가 포함된 약들을 찾아오는 명령어
    List<DrugInfo> findByNameContaining(String name);

}