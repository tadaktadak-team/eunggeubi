package com.tadaktadak.eunggeubi.domain.drug.controller;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.dto.DrugSearchPageResponse;
import com.tadaktadak.eunggeubi.domain.drug.service.DrugService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
            @RequestParam("keyword")
            @NotBlank(message = "검색어를 입력해주세요.")
            @Size(max = 50, message = "검색어는 50자 이내로 입력해주세요.") String keyword,
            @RequestParam(value = "pageNo", defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10")
            @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
            @Max(value = 100, message = "한 번에 최대 100건까지 조회할 수 있습니다.") int numOfRows) {

        DrugSearchPageResponse results = drugService.searchDrugsByName(keyword, pageNo, numOfRows);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{itemSeq}")
    public ResponseEntity<DrugInfoResponse> getDrugDetail(
            @PathVariable("itemSeq")
            @Pattern(regexp = "^\\d{1,20}$", message = "약품 번호가 올바르지 않습니다.") String itemSeq) {
        DrugInfoResponse response = drugService.getDrugDetail(itemSeq);
        return ResponseEntity.ok(response);
    }
}