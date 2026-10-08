package com.tadaktadak.eunggeubi.domain.drug.controller;

import com.tadaktadak.eunggeubi.domain.drug.dto.InteractionCheckRequest;
import com.tadaktadak.eunggeubi.domain.drug.dto.InteractionResponse;
import com.tadaktadak.eunggeubi.domain.drug.service.DurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drugs/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final DurService durService;

    // 선택된 약들(itemSeq 목록) 중 서로 병용금기인 쌍을 반환. 비어있으면 병용 가능.
    @PostMapping("/check")
    public ResponseEntity<List<InteractionResponse>> check(@RequestBody InteractionCheckRequest request) {
        List<InteractionResponse> conflicts = durService.checkInteractions(request.getItemSeqs());
        return ResponseEntity.ok(conflicts);
    }
}
