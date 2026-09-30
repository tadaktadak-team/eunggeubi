package com.tadaktadak.eunggeubi.domain.drug.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InteractionCheckRequest {
    private List<String> itemSeqs; // 사용자가 화면에서 고른 약들의 itemSeq 목록
}
