package com.tadaktadak.eunggeubi.domain.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalResponse {

    private Long id;          // 임시 ID
    private String name;      // 병원명
    private String address;   // 주소
    private Double latitude;  // 위도
    private Double longitude; // 경도
    private String type;      // 병원/약국
}