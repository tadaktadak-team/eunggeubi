package com.tadaktadak.eunggeubi.domain.first_aid.controller;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidGuideResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidSearchResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.SearchFirstAidGuideRequest;
import com.tadaktadak.eunggeubi.domain.first_aid.service.FirstAidGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig에서 permitAll - 응급처치 가이드는 로그인 여부와 상관없이 누구나 찾아볼 수 있어야 한다.
@RestController
@RequestMapping("/api/first-aid/guides")
@RequiredArgsConstructor
public class FirstAidGuideController {

    private final FirstAidGuideService firstAidGuideService;

    // 고를 수 있는 응급처치 주제 전체(first-aid-topics.txt). 경로 변수보다 우선 매칭되어 "/{situation}"과 안 겹친다.
    @GetMapping("/topics")
    public FirstAidSearchResponse topics() {
        return firstAidGuideService.listTopics();
    }

    // case는 세부 상황이 있는 주제(예: 동물·벌레 물림)에서 사용자가 고른 것 - 없으면 고를 목록(cases)이 온다.
    @GetMapping("/{situation}")
    public FirstAidGuideResponse getBySituation(@PathVariable String situation,
                                                @RequestParam(name = "case", required = false) String caseName) {
        return firstAidGuideService.getBySituation(situation, caseName);
    }

    // 매칭되는 상황이 없어도 404 대신 200 + 빈 candidates로 내려준다 - 프론트가 "못 찾음" 안내를 띄운다.
    @PostMapping("/search")
    public FirstAidSearchResponse search(@RequestBody SearchFirstAidGuideRequest request) {
        return firstAidGuideService.searchCandidates(request.text());
    }
}
