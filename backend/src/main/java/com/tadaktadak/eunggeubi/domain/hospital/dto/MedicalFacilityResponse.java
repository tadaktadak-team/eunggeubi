package com.tadaktadak.eunggeubi.domain.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFacilityResponse {

    private String ykiho;       // 암호화된 요양기호
    private String name;        // 병원명
    private String address;     // 주소
    private String phone;       // 전화번호
    private Double latitude;    // 위도
    private Double longitude;   // 경도
    private Double distance;
    private String type;        // 병원 종류
}