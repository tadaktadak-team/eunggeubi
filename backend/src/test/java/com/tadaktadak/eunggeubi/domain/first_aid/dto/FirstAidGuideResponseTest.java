package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FirstAidGuideResponseTest {

    private static final RawFirstAidGuide TWO_STEPS =
            new RawFirstAidGuide("t", List.of("압박합니다.", "병원을 방문합니다."));

    @Test
    void 순서가_필요하다고_판단된_여러_단계면_번호를_붙인다() {
        assertThat(FirstAidGuideResponse.of("열상", "t", TWO_STEPS, true).ordered()).isTrue();
    }

    @Test
    void 순서가_필요없다고_판단되면_번호를_안붙인다() {
        assertThat(FirstAidGuideResponse.of("골절", "t", TWO_STEPS, false).ordered()).isFalse();
    }

    // 번호는 순서가 꼭 필요하다고 판단된 처치에만 붙인다 - 판단에 실패(null)하면 번호 없이 둔다.
    @Test
    void 판단에_실패하면_번호를_안붙인다() {
        assertThat(FirstAidGuideResponse.of("열상", "t", TWO_STEPS, null).ordered()).isFalse();
    }

    @Test
    void 화면_이름은_짧은_이름_뒤_괄호에_원래_이름() {
        assertThat(new FirstAidTopic("열상", "베인 상처", "다침", "").label()).isEqualTo("베인 상처 (열상)");
        assertThat(new FirstAidTopic("골절", "골절", "다침", "").label()).isEqualTo("골절");
    }

    @Test
    void 한_단계뿐이면_순서가_없다() {
        assertThat(FirstAidGuideResponse.of("낙상", "t",
                new RawFirstAidGuide("t", List.of("즉시 병원을 방문합니다.")), true).ordered()).isFalse();
    }
}
