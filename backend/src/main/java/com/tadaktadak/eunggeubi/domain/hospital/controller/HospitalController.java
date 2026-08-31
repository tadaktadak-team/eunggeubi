package com.tadaktadak.eunggeubi.domain.hospital.controller;

import com.tadaktadak.eunggeubi.domain.hospital.dto.MedicalFacilityResponse;
import com.tadaktadak.eunggeubi.domain.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping(
            value = "/api/hospitals",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<MedicalFacilityResponse> findNearbyHospitals(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return hospitalService.findNearbyHospitals(lat, lng);
    }

    @GetMapping(
            value = "/api/pharmacies",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<MedicalFacilityResponse> findNearbyPharmacies(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return hospitalService.findNearbyPharmacies(lat, lng);
    }
}