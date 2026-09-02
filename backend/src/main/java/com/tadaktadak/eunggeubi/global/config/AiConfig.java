package com.tadaktadak.eunggeubi.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // spring-ai-starter-model-openai가 자동구성해주는 ChatClient.Builder를 그대로 build()해서 쓴다.
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
