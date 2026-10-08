package com.tadaktadak.eunggeubi.domain.health.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 복용약 한 건. itemSeq(품목일련번호)가 있으면 약 검색으로 고른 것, null이면 직접 입력한 것
public record MedicationItem(
        @NotBlank(message = "약 이름을 입력해주세요.")
        @Size(max = 200, message = "약 이름은 200자 이내로 입력해주세요.") String name,
        @Pattern(regexp = "^\\d{1,20}$", message = "약 번호가 올바르지 않습니다.") String itemSeq
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    public static List<MedicationItem> fromJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<List<MedicationItem>>() { });
        } catch (Exception e) {
            return List.of();
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
