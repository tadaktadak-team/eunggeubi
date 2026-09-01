package com.tadaktadak.eunggeubi.domain.ai_consultations.entity;

public enum ChecklistStatus {
    PROPOSED,     // AI가 생성해서 사용자 응답을 기다리는 중
    IN_PROGRESS,  // 일부만 응답됨 (지금은 submit이 한 번에 전체를 받아서 실제로는 안 쓰임 - 추후 부분 저장용)
    COMPLETED     // 사용자가 응답을 제출함
}
