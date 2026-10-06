package com.tadaktadak.eunggeubi.domain.drug.repository;

import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    // itemSeq와 mixtureItemSeq가 둘 다 선택된 약 목록 안에 있는 행만 가져온다.
    // 원본 데이터가 A→B, B→A 양방향으로 다 들어있을 수도 있어서, 방향을 따지지 않고
    // 양쪽 컬럼 다 같은 목록으로 검사하면 어느 쪽으로 저장돼 있든 걸러낼 수 있다.
    @Query("SELECT d FROM DrugInteraction d " +
            "WHERE d.itemSeq IN :itemSeqs AND d.mixtureItemSeq IN :itemSeqs")
    List<DrugInteraction> findConflictsWithin(@Param("itemSeqs") List<String> itemSeqs);
}
