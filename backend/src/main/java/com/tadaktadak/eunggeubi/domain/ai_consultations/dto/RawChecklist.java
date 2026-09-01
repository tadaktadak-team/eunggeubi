package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟) - 체크리스트 문항 생성용.
// 필드명을 "items"로 두면 JSON Schema의 array 타입 키워드("items")와 이름이 겹쳐서, 모델이
// 스키마 틀(type/properties/additionalProperties)까지 그대로 베껴 쓰고 그 안의 items만 문항으로
// 바꿔치기하는 오작동이 실제로 발생했다 (예: {"type":"object","properties":{"items":{"type":"array",
// "items":[...실제 문항...]}}} 형태로 응답). 그래서 충돌 없는 이름을 쓴다.
public record RawChecklist(List<String> questions) {
}
