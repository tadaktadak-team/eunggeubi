package com.tadaktadak.eunggeubi.domain.hospital.service;

import com.tadaktadak.eunggeubi.domain.hospital.dto.HospitalResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HospitalService {

    public List<HospitalResponse> findNearbyHospitals(double latitude, double longitude) {

        return List.of(
                HospitalResponse.builder()
                        .id(1L)
                        .name("서울대학교병원")
                        .address("서울특별시 종로구 대학로 101")
                        .latitude(37.5796)
                        .longitude(126.9989)
                        .type("병원")
                        .build(),

                HospitalResponse.builder()
                        .id(2L)
                        .name("강북삼성병원")
                        .address("서울특별시 종로구 새문안로 29")
                        .latitude(37.5683)
                        .longitude(126.9686)
                        .type("병원")
                        .build()
        );
    }
}