package com.tadaktadak.eunggeubi.domain.user.service;

import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 개인정보처리방침에 적은 보유기간이 지난 데이터를 매일 정리한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCleanupService {

    // 동의 토큰(3일)으로 마지막에 보낸 동의 링크(3일)까지 모두 만료된 뒤에 지운다
    static final Duration PENDING_SIGNUP_RETENTION = Duration.ofDays(7);
    // 탈퇴 후 부정 이용 확인을 위해 보관하는 기간
    static final Duration WITHDRAWN_RETENTION = Duration.ofDays(30);
    // 하루 발송 상한(최근 24시간)을 세는 기간이 지난 발송 기록만 지운다
    static final Duration ORPHANED_CONSENT_RETENTION = Duration.ofDays(1);

    // 회원 번호로 지울 데이터. 상담 체크리스트는 상담 메시지를 거쳐 찾으므로 상담보다 먼저 지운다
    private static final List<String> PURGE_QUERIES = List.of(
            "delete from ChecklistResponse r where r.userId in :ids or r.checklistId in"
                    + " (select c.id from Checklist c where c.consultationId in"
                    + " (select a.id from AiConsultation a where a.userId in :ids))",
            "delete from Checklist c where c.consultationId in"
                    + " (select a.id from AiConsultation a where a.userId in :ids)",
            "delete from AiConsultation a where a.userId in :ids",
            "delete from SearchHistory h where h.userId in :ids",
            "delete from GuardianConsent c where c.userId in :ids or c.guardianId in"
                    + " (select g.id from Guardian g where g.userId in :ids)",
            "delete from Guardian g where g.userId in :ids",
            "delete from HealthProfile p where p.userId in :ids",
            "delete from TermsAgreement t where t.userId in :ids",
            "delete from SocialAccount s where s.userId in :ids",
            "delete from RefreshToken t where t.userId in :ids",
            "delete from User u where u.id in :ids");

    private final UserRepository userRepository;
    private final GuardianConsentRepository guardianConsentRepository;
    private final EntityManager em;

    // 같은 객체 안에서 cleanUp 을 부르면 그쪽 @Transactional 이 적용되지 않아 여기에도 붙인다
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void runDaily() {
        cleanUp(LocalDateTime.now());
    }

    @Transactional
    public void cleanUp(LocalDateTime now) {
        // 보호자가 끝내 동의하지 않은 만 14세 미만 가입 신청. 로그인할 수 없어 스스로 탈퇴도 못 하므로 여기서 지운다.
        // 회원 행까지 지워야 같은 이메일로 다시 가입할 수 있다.
        int pendingSignups = purge(userRepository.findByStatusAndCreatedAtBefore(
                UserStatus.PENDING, now.minus(PENDING_SIGNUP_RETENTION)));
        int withdrawnUsers = purge(userRepository.findWithdrawnBefore(
                UserStatus.WITHDRAWN, now.minus(WITHDRAWN_RETENTION)));
        int orphanedConsents = guardianConsentRepository.deleteOrphanedSentBefore(now.minus(ORPHANED_CONSENT_RETENTION));
        log.info("보관기간 지난 데이터 정리: 가입 미완료 계정 {}건, 탈퇴 회원 {}건, 삭제된 보호자의 발송 기록 {}건",
                pendingSignups, withdrawnUsers, orphanedConsents);
    }

    private int purge(List<User> users) {
        if (users.isEmpty()) {   // 빈 목록으로 IN 조회를 하지 않는다
            return 0;
        }
        List<Long> ids = users.stream().map(User::getId).toList();
        em.flush();
        for (String jpql : PURGE_QUERIES) {
            em.createQuery(jpql).setParameter("ids", ids).executeUpdate();
        }
        em.clear();   // 일괄 삭제는 영속성 컨텍스트를 거치지 않아, 남아 있는 엔티티를 비운다
        return ids.size();
    }
}
