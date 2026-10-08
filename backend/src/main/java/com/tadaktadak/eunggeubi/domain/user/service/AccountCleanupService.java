package com.tadaktadak.eunggeubi.domain.user.service;

import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianConsentRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.TermsAgreementRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
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
    // 하루 발송 상한(최근 24시간)을 세는 기간이 지난 발송 기록만 지운다
    static final Duration ORPHANED_CONSENT_RETENTION = Duration.ofDays(1);

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianConsentRepository guardianConsentRepository;
    private final TermsAgreementRepository termsAgreementRepository;

    // 같은 객체 안에서 cleanUp 을 부르면 그쪽 @Transactional 이 적용되지 않아 여기에도 붙인다
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void runDaily() {
        cleanUp(LocalDateTime.now());
    }

    @Transactional
    public void cleanUp(LocalDateTime now) {
        int pendingSignups = deleteExpiredPendingSignups(now.minus(PENDING_SIGNUP_RETENTION));
        int orphanedConsents = guardianConsentRepository.deleteOrphanedSentBefore(now.minus(ORPHANED_CONSENT_RETENTION));
        log.info("보관기간 지난 데이터 정리: 가입 미완료 계정 {}건, 삭제된 보호자의 발송 기록 {}건", pendingSignups, orphanedConsents);
    }

    // 보호자가 끝내 동의하지 않은 만 14세 미만 가입 신청. 로그인할 수 없어 스스로 탈퇴도 못 하므로 여기서 지운다.
    // 회원 행까지 지워야 같은 이메일로 다시 가입할 수 있다.
    private int deleteExpiredPendingSignups(LocalDateTime createdBefore) {
        List<User> users = userRepository.findByStatusAndCreatedAtBefore(UserStatus.PENDING, createdBefore);
        if (users.isEmpty()) {   // 빈 목록으로 IN 조회를 하지 않는다
            return 0;
        }
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Guardian> guardians = guardianRepository.findByUserIdIn(userIds);
        if (!guardians.isEmpty()) {
            guardianConsentRepository.deleteByGuardianIdIn(guardians.stream().map(Guardian::getId).toList());
        }
        guardianConsentRepository.deleteByUserIdIn(userIds);
        guardianRepository.deleteAll(guardians);
        termsAgreementRepository.deleteByUserIdIn(userIds);
        userRepository.deleteAll(users);
        return users.size();
    }
}
