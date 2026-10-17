package com.tadaktadak.eunggeubi.domain.hospital.controller;

import com.tadaktadak.eunggeubi.domain.hospital.dto.MedicalFacilityResponse;
import com.tadaktadak.eunggeubi.domain.hospital.service.HospitalService;
import com.tadaktadak.eunggeubi.global.validation.KoreaLatitude;
import com.tadaktadak.eunggeubi.global.validation.KoreaLongitude;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.tadaktadak.eunggeubi.domain.hospital.dto.HospitalDetailResponse;

@RestController
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping(
            value = "/api/hospitals",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<MedicalFacilityResponse> findNearbyHospitals(
            @RequestParam @KoreaLatitude double lat,
            @RequestParam @KoreaLongitude double lng
    ) {
        return hospitalService.findNearbyHospitals(lat, lng);
    }

    @GetMapping(
            value = "/api/pharmacies",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<MedicalFacilityResponse> findNearbyPharmacies(
            @RequestParam @KoreaLatitude double lat,
            @RequestParam @KoreaLongitude double lng
    ) {
        return hospitalService.findNearbyPharmacies(lat, lng);
    }
    @GetMapping(
            value = "/api/hospitals/detail",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public HospitalDetailResponse getHospitalDetail(
            @RequestParam
            @Size(max = 200, message = "병원 식별자가 올바르지 않습니다.")
            @Pattern(regexp = "^[A-Za-z0-9+/=_-]+$", message = "병원 식별자가 올바르지 않습니다.") String ykiho
    ) {
        return hospitalService.findHospitalDetail(ykiho);
    }
}