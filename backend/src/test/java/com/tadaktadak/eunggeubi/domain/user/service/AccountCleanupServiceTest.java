package com.tadaktadak.eunggeubi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tadaktadak.eunggeubi.domain.user.entity.ConsentPurpose;
import com.tadaktadak.eunggeubi.domain.user.entity.ConsentStatus;
import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.GuardianConsent;
import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import com.tadaktadak.eunggeubi.domain.user.entity.TermsAgreement;
import com.tadaktadak.eunggeubi.domain.user.entity.TermsType;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.TermsAgreementRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.common.MessageType;
import com.tadaktadak.eunggeubi.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// 보관기간이 지난 가입 미완료 계정과 삭제된 보호자의 발송 기록을 지우고, 그 밖의 데이터는 남기는지 확인한다(내장 H2).
@DataJpaTest
@Import({AccountCleanupService.class, JpaAuditingConfig.class})
class AccountCleanupServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 4, 0);

    @Autowired private AccountCleanupService service;
    @Autowired private UserRepository userRepository;
    @Autowired private GuardianRepository guardianRepository;
    @Autowired private GuardianConsentRepository consentRepository;
    @Autowired private TermsAgreementRepository termsRepository;
    @Autowired private EntityManager em;

    private User user(String email, UserStatus status, LocalDateTime createdAt) {
        User user = userRepository.save(User.builder()
                .email(email).password("hashed").name("아이").phone("01011112222")
                .birthDate(LocalDate.of(2015, 1, 1)).gender(Gender.NONE).status(status)
                .build());
        // created_at 은 저장 시각으로 자동으로 채워져서 직접 과거로 돌린다
        em.flush();
        em.createNativeQuery("update users set created_at = ?1 where id = ?2")
                .setParameter(1, createdAt).setParameter(2, user.getId()).executeUpdate();
        return user;
    }

    private Guardian guardian(Long userId) {
        return guardianRepository.save(Guardian.builder()
                .userId(userId).name("엄마").phone("01033334444").relationship(Relationship.PARENT)
                .notifyEnabled(true).createdAt(NOW.minusDays(10)).build());
    }

    private GuardianConsent consent(Long guardianId, Long userId, LocalDateTime sentAt) {
        return consentRepository.save(GuardianConsent.builder()
                .guardianId(guardianId).userId(userId).phone("01033334444").purpose(ConsentPurpose.SIGNUP)
                .consentToken(UUID.randomUUID().toString()).status(ConsentStatus.PENDING)
                .messageType(MessageType.SMS).sentAt(sentAt).expiresAt(sentAt.plusDays(3)).build());
    }

    private void terms(Long userId) {
        termsRepository.save(TermsAgreement.builder()
                .userId(userId).termsType(TermsType.SERVICE).termsVersion("v1").agreed(true)
                .agreedAt(NOW.minusDays(10)).build());
    }

    private void cleanUp() {
        service.cleanUp(NOW);
        em.flush();
        em.clear();
    }

    @Test
    void 일주일이_지난_가입_미완료_계정은_보호자_동의기록_약관동의와_함께_지운다() {
        User stale = user("stale@example.com", UserStatus.PENDING, NOW.minusDays(8));
        Guardian guardian = guardian(stale.getId());
        consent(guardian.getId(), stale.getId(), NOW.minusDays(8));
        terms(stale.getId());

        cleanUp();

        assertThat(userRepository.findById(stale.getId())).isEmpty();
        assertThat(userRepository.existsByEmail("stale@example.com")).isFalse();   // 같은 이메일로 다시 가입할 수 있다
        assertThat(guardianRepository.findByUserId(stale.getId())).isEmpty();
        assertThat(consentRepository.countByUserIdAndSentAtAfter(stale.getId(), NOW.minusYears(1))).isZero();
        assertThat(termsRepository.findByUserId(stale.getId())).isEmpty();
    }

    @Test
    void 아직_동의를_기다릴_수_있는_계정과_가입이_끝난_계정은_남긴다() {
        User recent = user("recent@example.com", UserStatus.PENDING, NOW.minusDays(6));
        guardian(recent.getId());
        User active = user("active@example.com", UserStatus.ACTIVE, NOW.minusDays(30));
        guardian(active.getId());

        cleanUp();

        assertThat(userRepository.findById(recent.getId())).isPresent();
        assertThat(guardianRepository.findByUserId(recent.getId())).hasSize(1);
        assertThat(userRepository.findById(active.getId())).isPresent();
        assertThat(guardianRepository.findByUserId(active.getId())).hasSize(1);
    }

    @Test
    void 삭제된_보호자의_발송_기록은_하루가_지난_것만_지운다() {
        User active = user("active@example.com", UserStatus.ACTIVE, NOW.minusDays(30));
        Guardian deleted = guardian(active.getId());
        GuardianConsent old = consent(deleted.getId(), active.getId(), NOW.minusHours(25));
        GuardianConsent recent = consent(deleted.getId(), active.getId(), NOW.minusHours(23));
        guardianRepository.delete(deleted);
        Guardian kept = guardian(active.getId());
        GuardianConsent keptConsent = consent(kept.getId(), active.getId(), NOW.minusDays(5));

        cleanUp();

        assertThat(consentRepository.findById(old.getId())).isEmpty();
        // 최근 24시간 기록은 하루 발송 상한 계산에 쓰여서 남긴다
        assertThat(consentRepository.findById(recent.getId())).isPresent();
        assertThat(consentRepository.findById(keptConsent.getId())).isPresent();
    }
}
