package com.tadaktadak.eunggeubi.domain.user.repository;

import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    // 특정 회원에 등록된 보호자 목록 조회
    List<Guardian> findByUserId(Long userId);
}