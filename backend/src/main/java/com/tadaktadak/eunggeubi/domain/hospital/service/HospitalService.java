package com.tadaktadak.eunggeubi.domain.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tadaktadak.eunggeubi.domain.hospital.dto.HospitalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalService {

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;

    @Value("${openapi.hospital.url}")
    private String apiUrl;

    @Value("${openapi.hospital.service-key}")
    private String serviceKey;

    public List<HospitalResponse> findNearbyHospitals(
            double latitude,
            double longitude
    ) {

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(apiUrl)
                    .queryParam("ServiceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 20)
                    .queryParam("xPos", longitude)
                    .queryParam("yPos", latitude)
                    .queryParam("radius", 5000)
                    .build()
                    .toUriString();

            log.info("병원 API 요청: {}", apiUrl);

            String response = restTemplate.getForObject(url, String.class);

            System.out.println(response);

            return parseResponse(response);

        } catch (Exception e) {
            log.error("병원 API 호출 실패", e);
            throw new RuntimeException("병원 정보를 불러오지 못했습니다.");
        }
    }

    private List<HospitalResponse> parseResponse(String response)
            throws Exception {

        JsonNode root = xmlMapper.readTree(response);

        List<HospitalResponse> result = new ArrayList<>();

        JsonNode items = root
                .path("response")
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
                    HospitalResponse.builder()
                            .ykiho(text(item, "ykiho"))
                            .name(text(item, "yadmNm"))
                            .address(text(item, "addr"))
                            .phone(text(item, "telno"))
                            .latitude(doubleValue(item, "YPos"))
                            .longitude(doubleValue(item, "XPos"))
                            .type(text(item, "clCdNm"))
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