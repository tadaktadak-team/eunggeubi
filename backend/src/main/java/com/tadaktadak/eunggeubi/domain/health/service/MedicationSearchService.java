package com.tadaktadak.eunggeubi.domain.health.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchRequest;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchResponse;
import com.tadaktadak.eunggeubi.domain.drug.service.DrugService;
import com.tadaktadak.eunggeubi.domain.drug.service.PillService;
import com.tadaktadak.eunggeubi.domain.health.dto.MedicationSearchItem;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 복용약 선택용 검색. 낱알식별(전문의약품 포함)과 e약은요(액상·연고 포함)를 합쳐 품목번호 기준으로 중복을 없앤다
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationSearchService {

    private static final int MAX_RESULTS = 30;

    private final PillService pillService;
    private final DrugService drugService;

    public List<MedicationSearchItem> search(String keyword) {
        String query = keyword.replaceAll("\\s+", "");
        Map<String, MedicationSearchItem> merged = new LinkedHashMap<>();
        boolean pillFailed = false;
        boolean drugFailed = false;

        try {
            PillSearchRequest request = new PillSearchRequest();
            request.setItemName(query);
            for (PillSearchResponse pill : pillService.searchPills(request)) {
                if (pill.getItemSeq() != null && pill.getItemName() != null) {
                    merged.putIfAbsent(pill.getItemSeq(), new MedicationSearchItem(
                            pill.getItemSeq(), pill.getItemName().trim(), pill.getEtcOtcName(), pill.getItemImage()));
                }
            }
        } catch (ExternalApiException e) {
            pillFailed = true;
            log.warn("복용약 검색: 낱알식별 조회 실패 - {}", e.getMessage());
        }

        try {
            for (DrugInfoResponse drug : drugService.searchDrugsByName(query, 1, 20).getItems()) {
                if (drug.getItemSeq() != null && drug.getName() != null) {
                    merged.putIfAbsent(drug.getItemSeq(), new MedicationSearchItem(
                            drug.getItemSeq(), drug.getName().trim(), null, drug.getItemImage()));
                }
            }
        } catch (ExternalApiException e) {
            drugFailed = true;
            log.warn("복용약 검색: e약은요 조회 실패 - {}", e.getMessage());
        }

        if (pillFailed && drugFailed) {
            throw new ExternalApiException("약 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", null);
        }
        return merged.values().stream().limit(MAX_RESULTS).toList();
    }
}
