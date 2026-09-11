package com.tadaktadak.eunggeubi.domain.hospital.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HospitalDetailResponse {

    private String ykiho;

    // 요일별 진료시간 (HHmm 형식 문자열, 예: "0900")
    private String mondayStart;
    private String mondayEnd;
    private String tuesdayStart;
    private String tuesdayEnd;
    private String wednesdayStart;
    private String wednesdayEnd;
    private String thursdayStart;
    private String thursdayEnd;
    private String fridayStart;
    private String fridayEnd;
    private String saturdayStart;
    private String saturdayEnd;

    private String lunchTime;          // 점심시간 (예: "12:00~13:00")
    private String closedOnSunday;     // 일요일 진료 여부 ("휴무" 등)
    private String closedOnHoliday;    // 공휴일 진료 여부

    // 주차 정보
    private Integer parkingCapacity;   // 주차 가능 대수
    private String parkingFee;         // 유료 여부 (Y/N)
    private String parkingNote;        // 주차 요금 안내

    // 인근 지하철 정보
    private String nearestSubwayStation; // 역 이름
    private String subwayExit;           // 출구 번호
    private String subwayDistance;       // 거리

    // 진료과목 목록 (전문의 수 기준 내림차순 정렬)
    private List<DepartmentInfo> departments;

    @Getter
    @Builder
    public static class DepartmentInfo {
        private String name;         // 진료과목명
        private Integer doctorCount; // 전문의 수
    }
}