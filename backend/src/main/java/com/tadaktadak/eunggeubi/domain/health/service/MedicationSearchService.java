package com.tadaktadak.eunggeubi.domain.health.service;

import com.tadaktadak.eunggeubi.domain.drug.service.DrugService;
import com.tadaktadak.eunggeubi.domain.health.dto.MedicationSearchItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 복용약 선택용 검색. 약물 팀의 DrugService 한 곳만 쓴다 (DB 전환 뒤에도 같은 시그니처로 처방약까지 돌려준다)
@Service
@RequiredArgsConstructor
public class MedicationSearchService {

    private static final int MAX_RESULTS = 30;

    private final DrugService drugService;

    public List<MedicationSearchItem> search(String keyword) {
        String query = keyword.replaceAll("\\s+", "");
        return drugService.searchDrugsByName(query, 1, MAX_RESULTS).getItems().stream()
                .filter(d -> d.getItemSeq() != null && d.getName() != null)
                .map(d -> new MedicationSearchItem(d.getItemSeq(), d.getName().trim(), d.getDrugType(), d.getItemImage()))
                .toList();
    }
}
