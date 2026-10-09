package com.tadaktadak.eunggeubi.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 보관기간이 지난 데이터 정리(AccountCleanupService) 같은 예약 작업을 켠다
@EnableScheduling
@Configuration
public class SchedulingConfig {
}
