package com.tadaktadak.eunggeubi.domain.user.repository;

import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 시 이메일로 회원 조회
    Optional<User> findByEmail(String email);

    // 회원가입 시 이메일 중복 확인
    boolean existsByEmail(String email);

    // 보호자 동의를 끝내 받지 못한 가입 신청 정리용
    List<User> findByStatusAndCreatedAtBefore(UserStatus status, LocalDateTime before);

    // 아이디(이메일) 찾기: 이름 + 전화번호로 조회.
    // 전화번호 중복 가입을 막지 않으므로 결과가 여러 건일 수 있다 - Optional 로 받으면
    // NonUniqueResultException 이 터져 500 이 되므로 Top(1건)으로 제한한다.
    // 탈퇴 계정은 제외한다(탈퇴했는데 이메일을 돌려주면 계정이 살아있는 것처럼 보인다).
    Optional<User> findTopByNameAndPhoneAndStatusNotOrderByIdDesc(String name, String phone, UserStatus status);
}