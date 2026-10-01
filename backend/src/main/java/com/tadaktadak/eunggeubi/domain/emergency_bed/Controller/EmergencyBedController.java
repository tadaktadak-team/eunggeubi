package com.tadaktadak.eunggeubi.domain.emergency_bed.Controller;

import com.tadaktadak.eunggeubi.domain.emergency_bed.dto.EmergencyBedResponse;
import com.tadaktadak.eunggeubi.domain.emergency_bed.service.EmergencyBedService;
import com.tadaktadak.eunggeubi.global.validation.KoreaLatitude;
import com.tadaktadak.eunggeubi.global.validation.KoreaLongitude;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emergency-beds")
public class EmergencyBedController {

    private final EmergencyBedService emergencyBedService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EmergencyBedResponse> getEmergencyBeds(
            @RequestParam
            @Pattern(regexp = "^[가-힣]{2,15}$", message = "지역 이름이 올바르지 않습니다.") String stage1,
            @RequestParam @KoreaLatitude double latitude,
            @RequestParam @KoreaLongitude double longitude
    ) {
        return emergencyBedService.findNearbyEmergencyBeds(
                stage1,
                latitude,
                longitude
        );
    }
}