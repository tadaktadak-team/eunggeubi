package com.tadaktadak.eunggeubi.domain.drug.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.entity.DrugInfo;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 💡 데이터 변경 없이 '조회'만 할 때 서버 속도를 높여주는 옵션입니다.
public class DrugService {

    private final DrugInfoRepository drugInfoRepository;

    // 💡 약품명 검색 로직 (요구사항: MED01_SEARCH01)
    public List<DrugInfoResponse> searchDrugsByName(String keyword) {

        // 1. Repository를 통해 DB에서 키워드가 포함된 약 정보들을 모두 찾습니다.
        List<DrugInfo> drugs = drugInfoRepository.findByNameContaining(keyword);

        // 2. 찾은 Entity 정보(drugs)를 프론트엔드용 DTO 상자(DrugInfoResponse)로 포장해서 반환합니다.
        return drugs.stream()
                .map(DrugInfoResponse::from)
                .collect(Collectors.toList());
    }
}