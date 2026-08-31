package com.tadaktadak.eunggeubi.domain.emergency_bed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyBedResponse {

    private String hpid;              // 응급의료기관 ID
    private String name;              // 응급실명
    private String address;           // 주소
    private String phone;             // 응급실 전화번호

    private Double latitude;          // 위도
    private Double longitude;         // 경도
    private Double distance;          // 현재 위치와의 거리(km 또는 m)

    private Integer availableBeds;    // 현재 가용 응급실 일반병상
    private Integer standardBeds;     // 일반 기준병상
    private Integer congestion;       // 병상 기준 혼잡도(%)

    private String updatedAt;         // 데이터 갱신시간
}