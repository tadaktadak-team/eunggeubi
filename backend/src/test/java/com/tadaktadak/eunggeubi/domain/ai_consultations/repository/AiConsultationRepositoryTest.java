package com.tadaktadak.eunggeubi.domain.ai_consultations.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// @DataJpaTest는 내장 H2로 자동 교체되므로 MySQL/Qdrant/OPENAI_API_KEY 등 없이 단독 실행된다.
@DataJpaTest
class AiConsultationRepositoryTest {

    @Autowired
    private AiConsultationRepository repository;

    @Test
    void 세션_내_메시지를_생성순으로_조회한다() {
        AiConsultation user = repository.save(AiConsultation.builder()
                .sessionId("session-1").sessionRoot(true).guestCode("guest-1")
                .senderType(SenderType.USER).content("두통이 심해요").regenerated(false)
                .build());
        AiConsultation ai = repository.save(AiConsultation.builder()
                .sessionId("session-1").sessionRoot(false).guestCode("guest-1")
                .symptomKeyword("두통")
                .senderType(SenderType.AI).content("참고 정보 안내").regenerated(false)
                .parentId(user.getId())
                .build());

        List<AiConsultation> messages = repository.findBySessionIdOrderByCreatedAtAsc("session-1");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getSenderType()).isEqualTo(SenderType.USER);
        assertThat(messages.get(1).getId()).isEqualTo(ai.getId());
    }

    @Test
    void isOwnedBy는_userId또는_guestCode가_일치할때만_true() {
        AiConsultation guestMessage = AiConsultation.builder()
                .sessionId("s").sessionRoot(true).guestCode("guest-3")
                .senderType(SenderType.USER).content("어지러워요").regenerated(false)
                .build();

        assertThat(guestMessage.isOwnedBy(null, "guest-3")).isTrue();
        assertThat(guestMessage.isOwnedBy(null, "다른guest")).isFalse();
        assertThat(guestMessage.isOwnedBy(1L, null)).isFalse(); // 게스트 세션인데 로그인 사용자가 접근
    }
}
