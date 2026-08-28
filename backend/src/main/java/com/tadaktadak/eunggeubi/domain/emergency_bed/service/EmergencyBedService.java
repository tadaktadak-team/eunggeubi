package com.tadaktadak.eunggeubi.domain.emergency_bed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tadaktadak.eunggeubi.domain.emergency_bed.dto.EmergencyBedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyBedService {

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;

}