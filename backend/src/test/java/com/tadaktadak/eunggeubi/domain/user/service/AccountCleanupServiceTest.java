package com.tadaktadak.eunggeubi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.Checklist;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistStatus;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.auth.entity.Provider;
import com.tadaktadak.eunggeubi.domain.auth.entity.RefreshToken;
import com.tadaktadak.eunggeubi.domain.auth.entity.SocialAccount;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// 보관기간이 지난 가입 미완료 계정·탈퇴 회원과 삭제된 보호자의 발송 기록을 지우고, 그 밖의 데이터는 남기는지 확인한다(내장 H2).
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

    // 탈퇴 시각을 과거로 돌린다. null 이면 탈퇴 시각 칸이 생기기 전에 탈퇴한 회원(마지막 수정 시각으로 센다)
    private User withdrawn(String email, LocalDateTime withdrawnAt, LocalDateTime updatedAt) {
        User user = user(email, UserStatus.WITHDRAWN, NOW.minusDays(100));
        em.createNativeQuery("update users set withdrawn_at = ?1, updated_at = ?2 where id = ?3")
                .setParameter(1, withdrawnAt).setParameter(2, updatedAt).setParameter(3, user.getId()).executeUpdate();
        return user;
    }

    private long count(String entity, Long userId) {
        return em.createQuery("select count(e) from " + entity + " e where e.userId = :id", Long.class)
                .setParameter("id", userId).getSingleResult();
    }

    // 상담 메시지 + 체크리스트 + 응답 + 소셜 연동 + refresh 토큰 + 약관 동의를 만들고, 체크리스트 번호를 돌려준다
    private Long activityOf(Long userId) {
        AiConsultation message = em.merge(AiConsultation.builder()
                .sessionId("s-" + userId).sessionRoot(true).userId(userId)
                .senderType(SenderType.AI).content("참고 정보").regenerated(false).build());
        em.flush();
        Checklist checklist = em.merge(Checklist.builder()
                .consultationId(message.getId()).title("확인").items(List.of("열")).status(ChecklistStatus.COMPLETED).build());
        em.persist(ChecklistResponse.builder().checklistId(checklist.getId()).userId(userId).selectedItems(List.of("열")).build());
        em.persist(SocialAccount.builder().userId(userId).provider(Provider.KAKAO)
                .providerUserId("kakao-" + userId).linkedAt(NOW.minusDays(100)).build());
        em.persist(RefreshToken.builder().userId(userId).tokenHash("hash-" + userId)
                .issuedAt(NOW.minusDays(100)).expiresAt(NOW.minusDays(86)).build());
        terms(userId);
        em.flush();
        return checklist.getId();
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

    @Test
    void 탈퇴하고_30일이_지난_회원은_상담_내역과_연동_정보까지_모두_지운다() {
        User old = withdrawn("old@example.com", NOW.minusDays(31), NOW.minusDays(31));
        Long checklistId = activityOf(old.getId());

        cleanUp();

        assertThat(userRepository.findById(old.getId())).isEmpty();
        assertThat(count("AiConsultation", old.getId())).isZero();
        assertThat(count("ChecklistResponse", old.getId())).isZero();
        assertThat(em.find(Checklist.class, checklistId)).isNull();
        assertThat(count("SocialAccount", old.getId())).isZero();
        assertThat(count("RefreshToken", old.getId())).isZero();
        assertThat(count("TermsAgreement", old.getId())).isZero();
    }

    @Test
    void 탈퇴한_지_30일이_안_된_회원은_남긴다() {
        User recent = withdrawn("recent@example.com", NOW.minusDays(29), NOW.minusDays(29));
        activityOf(recent.getId());

        cleanUp();

        assertThat(userRepository.findById(recent.getId())).isPresent();
        assertThat(count("AiConsultation", recent.getId())).isEqualTo(1);
    }

    @Test
    void 탈퇴_시각이_없는_예전_탈퇴_회원은_마지막_수정_시각으로_센다() {
        User legacyOld = withdrawn("legacy-old@example.com", null, NOW.minusDays(40));
        User legacyRecent = withdrawn("legacy-recent@example.com", null, NOW.minusDays(5));

        cleanUp();

        assertThat(userRepository.findById(legacyOld.getId())).isEmpty();
        assertThat(userRepository.findById(legacyRecent.getId())).isPresent();
    }
}
