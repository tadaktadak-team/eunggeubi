package com.tadaktadak.eunggeubi.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "smsExecutor")
    public Executor smsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sms-");
        executor.initialize();
        return executor;
    }

    // AI 상담 스트리밍(AiConsultationController.consultStream). 답변 하나에 스레드 하나를 10~20초 붙잡는다
    // (RAG 검색 + LLM 스트림을 블로킹으로 소비). 넘치면 요청이 거절된다(500).
    @Bean(name = "consultStreamExecutor")
    public Executor consultStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("consult-stream-");
        executor.initialize();
        return executor;
    }

    // RagRetrievalService.retrieveGrouped의 해석별 동시 검색. consultStreamExecutor와 따로 둔다 - 같은 풀이면
    // 스트리밍 작업이 풀을 다 채운 채 자기 검색 작업을 기다려 서로 멈출 수 있다. 큐 없이 넘치면 호출한
    // 스레드가 직접 검색한다(CallerRunsPolicy) - 바쁠 때는 예전처럼 순서대로 검색하는 것과 같다.
    @Bean(name = "ragSearchExecutor")
    public Executor ragSearchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("rag-search-");
        executor.initialize();
        return executor;
    }
}