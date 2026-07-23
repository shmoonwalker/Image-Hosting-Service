package com.example.imagehostingservice.ai.gemini.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean(destroyMethod = "close")
    Client geminiClient(GeminiProperties properties) {
        return Client.builder()
                .apiKey(properties.getApiKey())
                .build();
    }
}