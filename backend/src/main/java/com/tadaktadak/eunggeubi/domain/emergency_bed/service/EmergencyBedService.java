package com.tadaktadak.eunggeubi.domain.emergency_bed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tadaktadak.eunggeubi.domain.emergency_bed.dto.EmergencyBedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyBedService {

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;

    @Value("${emergency_bed.url}")
    private String apiUrl;

    @Value("${emergency_bed.hospital-url}")
    private String hospitalApiUrl;

    @Value("${emergency_bed.secret}")
    private String serviceKey;

    /**
     * 현재 위치 주변의 응급실 병상 정보를 조회한다.
     *
     * @param stage1 시/도 (예: 서울특별시)
     * @param stage2 시/군/구 (예: 강남구)
     * @param userLatitude 사용자 위도
     * @param userLongitude 사용자 경도
     */
    public List<EmergencyBedResponse> findNearbyEmergencyBeds(
            String stage1,
            String stage2,
            double userLatitude,
            double userLongitude
    ) {

        try {
            // 1. 응급실 실시간 병상 정보 조회
            String bedResponse = getBedApiResponse(stage1, stage2);

            // 2. 응급의료기관 위치/기본 정보 조회
            String hospitalResponse = getHospitalApiResponse(stage1, stage2);

            // 3. 병원 정보는 hpid 기준으로 Map에 저장
            Map<String, JsonNode> hospitalMap =
                    parseHospitalInfo(hospitalResponse);

            // 4. 병상 정보 + 병원 정보 합치기
            List<EmergencyBedResponse> result =
                    parseEmergencyBeds(
                            bedResponse,
                            hospitalMap,
                            userLatitude,
                            userLongitude
                    );

            // 5. 가까운 응급실 순으로 정렬
            result.sort(
                    (a, b) -> Double.compare(
                            a.getDistance(),
                            b.getDistance()
                    )
            );

            return result;

        } catch (Exception e) {
            log.error("응급실 병상 정보 조회 실패", e);
            throw new RuntimeException(
                    "응급실 병상 정보를 불러오지 못했습니다."
            );
        }
    }

    /**
     * 응급실 실시간 병상 API 호출
     */
    private String getBedApiResponse(
            String stage1,
            String stage2
    ) {

        String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("STAGE1", stage1)
                .queryParam("STAGE2", stage2)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .build(false)
                .toUriString();

        log.info("응급실 병상 API 요청: {}", url);

        return restTemplate.getForObject(
                url,
                String.class
        );
    }

    /**
     * 응급의료기관 위치/기본 정보 API 호출
     */
    private String getHospitalApiResponse(
            String stage1,
            String stage2
    ) {

        String url = UriComponentsBuilder
                .fromHttpUrl(hospitalApiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("Q0", stage1)
                .queryParam("Q1", stage2)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .build(false)
                .toUriString();

        log.info("응급의료기관 정보 API 요청: {}", url);

        return restTemplate.getForObject(
                url,
                String.class
        );
    }

    /**
     * 병원 정보 API를 hpid 기준 Map으로 변환
     */
    private Map<String, JsonNode> parseHospitalInfo(
            String xml
    ) throws Exception {

        JsonNode root = xmlMapper.readTree(
                xml.getBytes(StandardCharsets.UTF_8)
        );

        JsonNode items = root
                .path("body")
                .path("items")
                .path("item");

        Map<String, JsonNode> hospitalMap = new HashMap<>();

        for (JsonNode item : toList(items)) {

            String hpid = item.path("hpid").asText();

            if (!hpid.isBlank()) {
                hospitalMap.put(hpid, item);
            }
        }

        return hospitalMap;
    }

    /**
     * 병상 API 응답을 EmergencyBedResponse로 변환
     */
    private List<EmergencyBedResponse> parseEmergencyBeds(
            String xml,
            Map<String, JsonNode> hospitalMap,
            double userLatitude,
            double userLongitude
    ) throws Exception {

        JsonNode root = xmlMapper.readTree(
                xml.getBytes(StandardCharsets.UTF_8)
        );

        JsonNode items = root
                .path("body")
                .path("items")
                .path("item");

        List<EmergencyBedResponse> result = new ArrayList<>();

        for (JsonNode item : toList(items)) {

            String hpid = item.path("hpid").asText();

            JsonNode hospital = hospitalMap.get(hpid);

            // 병원 위치 정보가 없으면 거리 계산이 불가능하므로 제외
            if (hospital == null) {
                continue;
            }

            String name = item.path("dutyName").asText();
            String phone = item.path("dutyTel3").asText();

            String address = hospital
                    .path("dutyAddr")
                    .asText();

            Double latitude = parseDouble(
                    hospital.path("wgs84Lat").asText()
            );

            Double longitude = parseDouble(
                    hospital.path("wgs84Lon").asText()
            );

            Integer availableBeds = parseInteger(
                    item.path("hvec").asText()
            );

            Integer standardBeds = parseInteger(
                    item.path("hvs01").asText()
            );

            Integer congestion = calculateCongestion(
                    availableBeds,
                    standardBeds
            );

            Double distance = calculateDistance(
                    userLatitude,
                    userLongitude,
                    latitude,
                    longitude
            );

            String updatedAt = item
                    .path("hvidate")
                    .asText();

            result.add(
                    EmergencyBedResponse.builder()
                            .hpid(hpid)
                            .name(name)
                            .address(address)
                            .phone(phone)
                            .latitude(latitude)
                            .longitude(longitude)
                            .distance(distance)
                            .availableBeds(availableBeds)
                            .standardBeds(standardBeds)
                            .congestion(congestion)
                            .updatedAt(updatedAt)
                            .build()
            );
        }

        return result;
    }

    /**
     * 혼잡도 계산
     *
     * 기준병상 - 현재 가용병상 = 사용 중인 병상
     *
     * 혼잡도 = 사용 중인 병상 / 기준병상 * 100
     */
    private Integer calculateCongestion(
            Integer availableBeds,
            Integer standardBeds
    ) {

        if (availableBeds == null
                || standardBeds == null
                || standardBeds <= 0) {

            return null;
        }

        int usedBeds = standardBeds - availableBeds;

        if (usedBeds < 0) {
            usedBeds = 0;
        }

        double congestion =
                ((double) usedBeds / standardBeds) * 100;

        return (int) Math.round(
                Math.min(congestion, 100)
        );
    }

    /**
     * 두 좌표 사이의 거리 계산
     * 결과: km
     */
    private Double calculateDistance(
            double userLatitude,
            double userLongitude,
            Double hospitalLatitude,
            Double hospitalLongitude
    ) {

        if (hospitalLatitude == null
                || hospitalLongitude == null) {

            return null;
        }

        final double EARTH_RADIUS = 6371.0;

        double latDistance = Math.toRadians(
                hospitalLatitude - userLatitude
        );

        double lonDistance = Math.toRadians(
                hospitalLongitude - userLongitude
        );

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)
                        +
                        Math.cos(Math.toRadians(userLatitude))
                                * Math.cos(Math.toRadians(hospitalLatitude))
                                * Math.sin(lonDistance / 2)
                                * Math.sin(lonDistance / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return Math.round(
                EARTH_RADIUS * c * 10
        ) / 10.0;
    }

    private Integer parseInteger(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * XML에서 item이 하나일 때도, 여러 개일 때도
     * List 형태로 처리
     */
    private List<JsonNode> toList(JsonNode node) {

        List<JsonNode> result = new ArrayList<>();

        if (node == null || node.isMissingNode()) {
            return result;
        }

        if (node.isArray()) {
            node.forEach(result::add);
        } else {
            result.add(node);
        }

        return result;
    }
}