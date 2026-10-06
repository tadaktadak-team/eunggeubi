package com.tadaktadak.eunggeubi.domain.drug.controller;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugSearchPageResponse;
import com.tadaktadak.eunggeubi.domain.drug.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    // 약품명 검색 API(http://localhost:8080/api/drugs/search?keyword=타이레놀&pageNo=1&numOfRows=10)
    @GetMapping("/search")
    public ResponseEntity<DrugSearchPageResponse> searchDrugs(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows) {

        DrugSearchPageResponse results = drugService.searchDrugsByName(keyword, pageNo, numOfRows);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{itemSeq}")
    public ResponseEntity<DrugInfoResponse> getDrugDetail(@PathVariable("itemSeq") String itemSeq) {
        DrugInfoResponse response = drugService.getDrugDetail(itemSeq);
        return ResponseEntity.ok(response);
    }
}