package com.tadaktadak.eunggeubi.domain.emergency_bed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tadaktadak.eunggeubi.domain.emergency_bed.dto.EmergencyBedResponse;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
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
            Map<String, JsonNode> hospitalMap = parseHospitalInfo(hospitalResponse);


            //  결과가 안 나올 때 어느 쪽 API가 문제인지 바로 알 수 있게 로그를 남깁니다.
            if (hospitalMap.isEmpty()) {
                log.warn("응급의료기관 위치정보가 0건입니다. (stage1={}, stage2={}) "
                        + "→ 병상 정보가 있어도 전부 제외됩니다.", stage1, stage2);
            } else {
                log.info("응급의료기관 위치정보 {}건 로드", hospitalMap.size());
            }

            // 4. 병상 정보 + 병원 정보 합치기
            List<EmergencyBedResponse> result =
                    parseEmergencyBeds(bedResponse, hospitalMap, userLatitude, userLongitude);

            // 5. 가까운 응급실 순으로 정렬

            result.sort(Comparator.comparingDouble(EmergencyBedResponse::getDistance));

            return result;

        } catch (Exception e) {
            log.error("응급실 병상 정보 조회 실패", e);
            // [수정 4] RuntimeException → ExternalApiException
            //  GlobalExceptionHandler가 502 + {"message": ...} 형식으로 응답해 줍니다.
            throw new ExternalApiException("응급실 병상 정보를 불러오지 못했습니다.", e);
        }
    }

    /**
     * 응급실 실시간 병상 API 호출
     */
    private String getBedApiResponse(String stage1, String stage2) {
        // [수정 5] 핵심 수정 ★ build(false) + toUriString() → build(true) + toUri()
        //  기존 코드는 인코딩되지 않은 "String"을 RestTemplate에 넘겼는데,
        //  RestTemplate은 String URL을 받으면 내부에서 한 번 더 인코딩합니다.
        //  그래서 Encoding 키의 '%'가 '%25'로 바뀌어 SERVICE_KEY_IS_NOT_REGISTERED_ERROR가 납니다.
        //  (build(false)로는 이 재인코딩을 막을 수 없습니다)
        //  → URI 객체로 넘기면 RestTemplate이 손대지 않으므로 인코딩이 정확히 한 번만 일어납니다.
        URI uri = UriComponentsBuilder
                .fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("STAGE1", encode(stage1))
                .queryParam("STAGE2", encode(stage2))
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .build(true)
                .toUri();

        return fetch(uri, "응급실 병상");
    }

    /**
     * 응급의료기관 위치/기본 정보 API 호출
     */
    private String getHospitalApiResponse(String stage1, String stage2) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(hospitalApiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("Q0", encode(stage1))
                .queryParam("Q1", encode(stage2))
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .build(true)
                .toUri();

        return fetch(uri, "응급의료기관 정보");
    }

    // [수정 6] 두 API 호출의 공통 부분(요청/디코딩)을 헬퍼로 분리했습니다.
    private String fetch(URI uri, String label) {
        log.info("{} API 요청: {}", label, uri);

        // [수정 7] String.class → byte[].class
        //  new RestTemplate()의 기본 String 변환기는 charset이 ISO-8859-1이라 한글이 깨집니다.
        //  (HospitalService도 같은 이유로 byte[]로 받아 UTF-8로 직접 디코딩합니다)
        byte[] bytes = restTemplate.getForObject(uri, byte[].class);
        if (bytes == null) {
            throw new IllegalStateException(label + " API 응답이 비어 있습니다.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // [수정 8] build(true)는 "값이 이미 인코딩되어 있다"고 간주하므로,
    //  한글 파라미터(서울특별시/강남구)는 여기서 직접 인코딩해 줘야 합니다.
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 병원 정보 API를 hpid 기준 Map으로 변환
     */
    private Map<String, JsonNode> parseHospitalInfo(String xml) throws Exception {
        // [수정 9] xml.getBytes(UTF_8) 제거
        //  이미 UTF-8로 올바르게 디코딩된 문자열이라 다시 바이트로 바꿀 필요가 없습니다.
        //  (기존 코드는 '깨진 문자열'을 다시 인코딩하는 것이라 한글 복구가 되지 않았습니다)
        JsonNode root = xmlMapper.readTree(xml);

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

        JsonNode root = xmlMapper.readTree(xml);

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

            Double latitude = parseDouble(hospital.path("wgs84Lat").asText());
            Double longitude = parseDouble(hospital.path("wgs84Lon").asText());

            // [수정 10] 좌표가 비어 있는 기관도 실제로 존재합니다.
            //  여기서 걸러야 distance가 null이 되지 않아 위 정렬에서 NPE가 나지 않습니다.
            if (latitude == null || longitude == null) {
                continue;
            }

            String name = item.path("dutyName").asText();
            String phone = item.path("dutyTel3").asText();
            String address = hospital.path("dutyAddr").asText();

            Integer availableBeds = parseInteger(item.path("hvec").asText());
            Integer standardBeds = parseInteger(item.path("hvs01").asText());
            Integer congestion = calculateCongestion(availableBeds, standardBeds);

            Double distance = calculateDistance(userLatitude, userLongitude, latitude, longitude);

            String updatedAt = item.path("hvidate").asText();

            result.add(
                    EmergencyBedResponse.builder()
                            .hpid(hpid)
                            .name(name)
                            .address(address)
                            // [수정 11] .phone(콜) → .phone(콜)  (오타로 컴파일이 안 되던 부분)
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
     * 기준병상 - 현재 가용병상 = 사용 중인 병상
     * 혼잡도 = 사용 중인 병상 / 기준병상 * 100
     *
     * 참고: hvec(가용병상)은 초과 수용 시 음수로 내려옵니다(실제 응답에서 -7 확인).
     *      이 경우 혼잡도는 100%로 캡핑됩니다.
     */
    private Integer calculateCongestion(Integer availableBeds, Integer standardBeds) {
        if (availableBeds == null || standardBeds == null || standardBeds <= 0) {
            return null;
        }

        int usedBeds = standardBeds - availableBeds;
        if (usedBeds < 0) {
            usedBeds = 0;
        }

        double congestion = ((double) usedBeds / standardBeds) * 100;
        return (int) Math.round(Math.min(congestion, 100));
    }

    /**
     * 두 좌표 사이의 거리 계산 (km)
     */
    private Double calculateDistance(
            double userLatitude,
            double userLongitude,
            Double hospitalLatitude,
            Double hospitalLongitude
    ) {
        if (hospitalLatitude == null || hospitalLongitude == null) {
            return null;
        }

        final double EARTH_RADIUS = 6371.0;

        double latDistance = Math.toRadians(hospitalLatitude - userLatitude);
        double lonDistance = Math.toRadians(hospitalLongitude - userLongitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLatitude))
                * Math.cos(Math.toRadians(hospitalLatitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(EARTH_RADIUS * c * 10) / 10.0;
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
     * XML에서 item이 하나일 때도, 여러 개일 때도 List로 처리
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