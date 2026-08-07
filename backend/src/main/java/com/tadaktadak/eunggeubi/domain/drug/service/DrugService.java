package com.tadaktadak.eunggeubi.domain.drug.service;

import com.tadaktadak.eunggeubi.domain.drug.dto.DrugInfoResponse;
import com.tadaktadak.eunggeubi.domain.drug.repository.DrugInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrugService {

    private final DrugInfoRepository drugInfoRepository;

    // 약품명 검색 (프론트엔드 개발용 Mock 데이터)
    public List<DrugInfoResponse> searchDrugsByName(String keyword) {
        List<DrugInfoResponse> mockList = new ArrayList<>();

        // 키워드 기본값 처리
        String searchKey = (keyword == null || keyword.isBlank()) ? "일반" : keyword;

        // 1. 대표 Mock 약품 1: 타이레놀
        mockList.add(DrugInfoResponse.builder()
                .itemSeq("199300001")
                .name("타이레놀정500밀리그램 (" + searchKey + " 연관)")
                .shape("장원형")
                .color("하얀색")
                .imprint("TYLENOL 500")
                .efficacy("감기로 인한 발열 및 통증, 두통, 신경통, 근육통 완화")
                .useInfo("성인 1회 1~2정씩 1일 3~4회 (4~6시간 간격) 필요시 복용")
                .caution("매일 세 잔 이상 정기적으로 술을 마시는 사람이 이 약을 복용할 경우 간 손상이 유발될 수 있습니다.")
                .drugType("일반의약품")
                .build());

        // 2. 대표 Mock 약품 2: 게보린
        mockList.add(DrugInfoResponse.builder()
                .itemSeq("198000002")
                .name("게보린정 (" + searchKey + " 연관)")
                .shape("삼각형")
                .color("분홍색")
                .imprint("GBN")
                .efficacy("두통, 치통, 생리통, 신경통 완화")
                .useInfo("성인 1회 1정 1일 3회까지 복용 (복용 간격은 4시간 이상)")
                .caution("만 15세 미만의 소아는 복용하지 않도록 합니다.")
                .drugType("일반의약품")
                .build());

        // 3. 대표 Mock 약품 3: 아스피린
        mockList.add(DrugInfoResponse.builder()
                .itemSeq("199800003")
                .name("바이엘아스피린정500밀리그램")
                .shape("원형")
                .color("하얀색")
                .imprint("BAYER")
                .efficacy("혈전 예방, 두통, 관절통, 고열 완화")
                .useInfo("성인 1회 1~2정씩 1일 2~3회 복용")
                .caution("위장관 출혈 환자나 수술을 앞둔 환자는 복용을 금합니다.")
                .drugType("일반의약품")
                .build());

        return mockList;
    }
    public DrugInfoResponse getDrugDetail(String itemSeq) {
        return DrugInfoResponse.builder()
                .itemSeq(itemSeq)
                .name("타이레놀정500밀리그램 (상세조회)")
                .shape("장원형")
                .color("하얀색")
                .imprint("TYLENOL 500")
                .efficacy("감기로 인한 발열 및 통증, 두통, 신경통, 근육통 완화")
                .useInfo("성인 1회 1~2정씩 1일 3~4회 (4~6시간 간격) 필요시 복용")
                .caution("매일 세 잔 이상 정기적으로 술을 마시는 사람이 이 약을 복용할 경우 간 손상이 유발될 수 있습니다.")
                .drugType("일반의약품")
                .build();
    }
}