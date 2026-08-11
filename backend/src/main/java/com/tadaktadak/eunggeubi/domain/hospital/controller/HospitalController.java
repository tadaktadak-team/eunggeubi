package com.tadaktadak.eunggeubi.domain.hospital.controller;

import com.tadaktadak.eunggeubi.domain.hospital.dto.HospitalResponse;
import com.tadaktadak.eunggeubi.domain.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping("/api/hospitals")
    public List<HospitalResponse> findNearbyHospitals(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return hospitalService.findNearbyHospitals(lat, lng);
    }
    @GetMapping("/api/pharmacies")
    public List<HospitalResponse> findNearbyPharmacies(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return hospitalService.findNearbyPharmacies(lat, lng);
    }
}