package com.tadaktadak.eunggeubi.domain.emergency_bed.Controller;

import com.tadaktadak.eunggeubi.domain.emergency_bed.dto.EmergencyBedResponse;
import com.tadaktadak.eunggeubi.domain.emergency_bed.service.EmergencyBedService;
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
            @RequestParam String stage1,
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return emergencyBedService.findNearbyEmergencyBeds(
                stage1,
                latitude,
                longitude
        );
    }
}