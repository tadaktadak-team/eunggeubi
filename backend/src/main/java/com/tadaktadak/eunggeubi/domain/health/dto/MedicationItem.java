package com.tadaktadak.eunggeubi.domain.health.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 복용약 한 건. itemSeq(품목일련번호)가 있으면 약 검색으로 고른 것, null이면 직접 입력한 것
public record MedicationItem(
        @NotBlank(message = "약 이름을 입력해주세요.")
        @Size(max = 200, message = "약 이름은 200자 이내로 입력해주세요.") String name,
        @Pattern(regexp = "^\\d{1,20}$", message = "약 번호가 올바르지 않습니다.") String itemSeq
) {
    private static final Logger log = LoggerFactory.getLogger(MedicationItem.class);
    // 나중에 항목(예: 용량)이 늘어난 데이터를 예전 서버가 읽어도 실패하지 않게, 모르는 필드는 무시한다
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static List<MedicationItem> fromNames(List<String> names) {
        if (names == null) {
            return List.of();
        }
        // 목록 안에 null 이나 빈 값이 섞여 와도(예: [null]) 서버 오류 없이 걸러낸다
        return names.stream().filter(n -> n != null && !n.isBlank()).map(n -> new MedicationItem(n, null)).toList();
    }

    // AI 상담이 읽는 콤마 문자열(이름만)에서 복원할 때 쓴다
    public static List<MedicationItem> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return fromNames(Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    // DB에 저장된 복용약을 복원한다. 번호까지 담은 JSON(medication_items)이 있으면 그걸 쓰고,
    // 없거나 읽을 수 없으면 이름만 담은 콤마 문자열(medications)로 복원한다.
    // 읽기에 실패했을 때 빈 목록을 돌려주면 사용자에게는 복용약이 사라진 것처럼 보이고, 그 상태로 저장하면
    // 실제로 지워진다. 그래서 이름이라도 되살리고, 원인을 찾을 수 있게 로그를 남긴다.
    public static List<MedicationItem> fromStored(String json, String csv) {
        if (json == null || json.isBlank()) {
            return fromCsv(csv);
        }
        try {
            List<MedicationItem> items = MAPPER.readValue(json, new TypeReference<List<MedicationItem>>() { });
            return items == null ? fromCsv(csv) : items;
        } catch (Exception e) {
            // 예외 메시지에는 JSON 내용(건강 정보)의 일부가 들어갈 수 있어 종류와 길이만 남긴다
            log.warn("복용약 JSON(medication_items)을 읽지 못해 이름만 복원합니다: {}, length={}",
                    e.getClass().getSimpleName(), json.length());
            return fromCsv(csv);
        }
    }

    public static String toJson(List<MedicationItem> items) {
        try {
            return MAPPER.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("복용약을 저장하지 못했습니다.", e);
        }
    }

    // 이름만 콤마로 이은 문자열. 이름 안의 콤마는 구분과 헷갈리지 않게 공백으로 바꾼다
    public static String toCsv(List<MedicationItem> items) {
        if (items.isEmpty()) {
            return null;
        }
        return String.join(",", items.stream().map(i -> i.name().replace(',', ' ')).toList());
    }

    // 같은 약(번호가 같거나, 번호가 없으면 이름이 같은 것)은 한 번만 남긴다
    public static List<MedicationItem> distinct(List<MedicationItem> items) {
        Map<String, MedicationItem> unique = new LinkedHashMap<>();
        for (MedicationItem item : items) {
            if (item == null || item.name() == null) {
                continue;
            }
            String name = item.name().trim();
            if (name.isEmpty()) {
                continue;
            }
            unique.putIfAbsent(item.itemSeq() != null ? "seq:" + item.itemSeq() : "name:" + name,
                    new MedicationItem(name, item.itemSeq()));
        }
        return List.copyOf(unique.values());
    }
}
