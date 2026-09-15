package com.tadaktadak.eunggeubi.domain.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tadaktadak.eunggeubi.domain.hospital.dto.MedicalFacilityResponse;
import com.tadaktadak.eunggeubi.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.tadaktadak.eunggeubi.domain.hospital.dto.HospitalDetailResponse;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalService {

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${medical_locator.hospital.url}")
    private String apiUrl;

    @Value("${medical_locator.pharmacy.url}")
    private String pharmacyApiUrl;

    @Value("${medical_locator.secret}")
    private String serviceKey;

    @Value("${medical_locator.detail.url}")
    private String detailApiUrl;

    @Value("${medical_locator.subject.url}")
    private String subjectApiUrl;

    public List<MedicalFacilityResponse> findNearbyHospitals(
            double latitude,
            double longitude
    ) {
        return findNearby(
                apiUrl,
                serviceKey,
                latitude,
                longitude,
                "병원"
        );
    }

    public List<MedicalFacilityResponse> findNearbyPharmacies(
            double latitude,
            double longitude
    ) {
        return findNearby(
                pharmacyApiUrl,
                serviceKey,
                latitude,
                longitude,
                "약국"
        );
    }

    /**
     * 병원 상세정보(진료시간, 진료과목) 조회
     */
    public HospitalDetailResponse findHospitalDetail(String ykiho) {
        try {
            String detailResponse = getDetailApiResponse(ykiho);
            String subjectResponse = getSubjectApiResponse(ykiho);

            JsonNode detailRoot = xmlMapper.readTree(detailResponse);
            JsonNode detailItem = detailRoot.path("body").path("items").path("item");

            List<HospitalDetailResponse.DepartmentInfo> departments =
                    parseDepartments(subjectResponse);

            return HospitalDetailResponse.builder()
                    .ykiho(ykiho)
                    .mondayStart(text(detailItem, "trmtMonStart"))
                    .mondayEnd(text(detailItem, "trmtMonEnd"))
                    .tuesdayStart(text(detailItem, "trmtTueStart"))
                    .tuesdayEnd(text(detailItem, "trmtTueEnd"))
                    .wednesdayStart(text(detailItem, "trmtWedStart"))
                    .wednesdayEnd(text(detailItem, "trmtWedEnd"))
                    .thursdayStart(text(detailItem, "trmtThuStart"))
                    .thursdayEnd(text(detailItem, "trmtThuEnd"))
                    .fridayStart(text(detailItem, "trmtFriStart"))
                    .fridayEnd(text(detailItem, "trmtFriEnd"))
                    .saturdayStart(text(detailItem, "trmtSatStart"))
                    .saturdayEnd(text(detailItem, "trmtSatEnd"))
                    .lunchTime(text(detailItem, "lunchWeek"))
                    .closedOnSunday(text(detailItem, "noTrmtSun"))
                    .closedOnHoliday(text(detailItem, "noTrmtHoli"))
                    .parkingCapacity(intValue(detailItem, "parkQty"))
                    .parkingFee(text(detailItem, "parkXpnsYn"))
                    .parkingNote(text(detailItem, "parkEtc"))
                    .nearestSubwayStation(text(detailItem, "plcNm"))
                    .subwayExit(text(detailItem, "plcDir"))
                    .subwayDistance(text(detailItem, "plcDist"))
                    .departments(departments)
                    .build();

        } catch (Exception e) {
            log.error("병원 상세정보 조회 실패 (ykiho={})", ykiho, e);
            throw new ExternalApiException("병원 상세정보를 불러오지 못했습니다.", e);
        }
    }

    private String getDetailApiResponse(String ykiho) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(detailApiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("ykiho", ykiho)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .build(true)
                .toUri();

        byte[] responseBytes = restTemplate.getForObject(uri, byte[].class);
        return new String(responseBytes, StandardCharsets.UTF_8);
    }

    private String getSubjectApiResponse(String ykiho) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(subjectApiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("ykiho", ykiho)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 30)
                .build(true)
                .toUri();

        byte[] responseBytes = restTemplate.getForObject(uri, byte[].class);
        return new String(responseBytes, StandardCharsets.UTF_8);
    }

    private List<HospitalDetailResponse.DepartmentInfo> parseDepartments(String response)
            throws Exception {

        JsonNode root = xmlMapper.readTree(response);
        JsonNode items = root.path("body").path("items").path("item");

        if (items.isObject()) {
            items = xmlMapper.createArrayNode().add(items);
        }

        List<HospitalDetailResponse.DepartmentInfo> result = new ArrayList<>();

        if (!items.isArray()) {
            return result;
        }

        for (JsonNode item : items) {
            result.add(
                    HospitalDetailResponse.DepartmentInfo.builder()
                            .name(text(item, "dgsbjtCdNm"))
                            .doctorCount(intValue(item, "dgsbjtPrSdrCnt"))
                            .build()
            );
        }

        result.sort(Comparator.comparing(
                HospitalDetailResponse.DepartmentInfo::getDoctorCount,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return result;
    }

    private Integer intValue(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final int NEARBY_RADIUS_METERS = 3000;
    private static final int NEARBY_MAX_ROWS = 100;
    private static final int NEARBY_RESULT_LIMIT = 20;

    private List<MedicalFacilityResponse> findNearby(
            String apiUrl,
            String serviceKey,
            double latitude,
            double longitude,
            String type
    ) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(apiUrl)
                    .queryParam("ServiceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", NEARBY_MAX_ROWS)
                    .queryParam("xPos", longitude)
                    .queryParam("yPos", latitude)
                    .queryParam("radius", NEARBY_RADIUS_METERS)
                    .build(true)
                    .toUri();

            byte[] responseBytes = restTemplate.getForObject(uri, byte[].class);

            String response =
                    new String(responseBytes, StandardCharsets.UTF_8);

            List<MedicalFacilityResponse> result = parseResponse(response);
            result.sort(Comparator.comparingDouble(
                    item -> item.getDistance() == null ? Double.MAX_VALUE : item.getDistance()
            ));

            if (result.size() > NEARBY_RESULT_LIMIT) {
                result = result.subList(0, NEARBY_RESULT_LIMIT);
            }

            return result;

        } catch (Exception e) {
            log.error("{} API 호출 실패", type, e);
            throw new ExternalApiException(type + " 정보를 불러오지 못했습니다.", e);
        }
    }

    private List<MedicalFacilityResponse> parseResponse(String response)
            throws Exception {

        JsonNode root = xmlMapper.readTree(response);

        List<MedicalFacilityResponse> result = new ArrayList<>();

        JsonNode items = root
                .path("body")
                .path("items")
                .path("item");

        if (items.isObject()) {
            items = xmlMapper.createArrayNode().add(items);
        }

        if (!items.isArray()) {
            return result;
        }

        for (JsonNode item : items) {
            result.add(
                    MedicalFacilityResponse.builder()
                            .ykiho(text(item, "ykiho"))
                            .name(text(item, "yadmNm"))
                            .address(text(item, "addr"))
                            .phone(text(item, "telno"))
                            .latitude(doubleValue(item, "YPos"))
                            .longitude(doubleValue(item, "XPos"))
                            .type(text(item, "clCdNm"))
                            .distance(toKm(doubleValue(item, "distance")))
                            .build()
            );
        }

        return result;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull()
                ? null
                : value.asText();
    }

    private Double toKm(Double meters) {
        if (meters == null) {
            return null;
        }
        return Math.round(meters / 1000 * 10) / 10.0;
    }

    private Double doubleValue(JsonNode node, String field) {
        String value = text(node, field);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}