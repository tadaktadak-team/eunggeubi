package com.tadaktadak.eunggeubi.domain.drug.controller;

import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchRequest;
import com.tadaktadak.eunggeubi.domain.drug.dto.PillSearchPageResponse;
import com.tadaktadak.eunggeubi.domain.drug.service.PillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/drugs/pills")
@RequiredArgsConstructor
public class PillController {

    private final PillService pillService;

    @GetMapping("/identification")
    public ResponseEntity<PillSearchPageResponse> searchPills(@ModelAttribute PillSearchRequest request) {
        PillSearchPageResponse responses = pillService.searchPills(request);
        return ResponseEntity.ok(responses);
    }
}