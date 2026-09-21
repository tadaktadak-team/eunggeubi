package com.tadaktadak.eunggeubi.domain.first_aid.controller;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidGuideResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.SearchFirstAidGuideRequest;
import com.tadaktadak.eunggeubi.domain.first_aid.service.FirstAidGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig에서 permitAll - 응급처치 가이드는 로그인 여부와 상관없이 누구나 찾아볼 수 있어야 한다.
@RestController
@RequestMapping("/api/first-aid/guides")
@RequiredArgsConstructor
public class FirstAidGuideController {

    private final FirstAidGuideService firstAidGuideService;

    @GetMapping("/{situation}")
    public FirstAidGuideResponse getBySituation(@PathVariable String situation) {
        return firstAidGuideService.getBySituation(situation);
    }

    // 매칭되는 상황이 없으면 프론트가 null을 기대하므로(FirstAidGuideDto | null), 404 대신 200 + null
    // 본문으로 내려준다.
    @PostMapping("/search")
    public ResponseEntity<FirstAidGuideResponse> search(@RequestBody SearchFirstAidGuideRequest request) {
        return ResponseEntity.ok(firstAidGuideService.search(request.text()).orElse(null));
    }
}
