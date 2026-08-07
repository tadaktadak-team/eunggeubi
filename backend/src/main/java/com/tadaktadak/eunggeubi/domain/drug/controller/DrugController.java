package com.tadaktadak.eunggeubi.domain.drug.controller;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    // 약품명 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<DrugInfoResponse>> searchDrugs(@RequestParam("keyword") String keyword) {

        // 이 키워드로 약 좀 찾아줘
        List<DrugInfoResponse> results = drugService.searchDrugsByName(keyword);
        return ResponseEntity.ok(results);
    }
    @GetMapping("/{itemSeq}")
    public ResponseEntity<DrugInfoResponse> getDrugDetail(@PathVariable("itemSeq") String itemSeq) {
        DrugInfoResponse response = drugService.getDrugDetail(itemSeq);
        return ResponseEntity.ok(response);
    }
}