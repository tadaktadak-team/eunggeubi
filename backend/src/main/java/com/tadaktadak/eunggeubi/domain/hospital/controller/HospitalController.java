package com.tadaktadak.eunggeubi.domain.hospital.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @GetMapping(
            value = "/api/hospitals",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String findNearbyHospitals(
            @RequestParam double lat,
            @RequestParam double lng
    ) throws JsonProcessingException {

        List<MedicalFacilityResponse> hospitals =
                hospitalService.findNearbyHospitals(lat, lng);

        return objectMapper.writeValueAsString(hospitals);
    }

    @GetMapping(
            value = "/api/pharmacies",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String findNearbyPharmacies(
            @RequestParam double lat,
            @RequestParam double lng
    ) throws JsonProcessingException {

        List<MedicalFacilityResponse> pharmacies =
                hospitalService.findNearbyPharmacies(lat, lng);

        return objectMapper.writeValueAsString(pharmacies);
    }
}