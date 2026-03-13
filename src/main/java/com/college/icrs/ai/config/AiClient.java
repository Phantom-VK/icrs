package com.college.icrs.ai.config;


import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@lombok.RequiredArgsConstructor
@Slf4j
public class AiClient {

    @Value("${ai.apikey}")
    private String apiKey;

    @Value("${ai.baseurl}")
    private String baseUrl;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;

    private final IcrsProperties icrsProperties;

    @Bean
    public ChatModel chatModel() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API key missing. Set ai.apikey or DEEPSEEK_API_KEY/OPENAI_API_KEY.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("AI base URL missing. Set ai.baseurl or DEEPSEEK_API_BASE/OPENAI_API_BASE.");
        }

        log.info(IcrsLog.event("ai.chat-model.initialized",
                "modelName", modelName,
                "baseUrl", normalizeBaseUrl(baseUrl),
                "timeoutSeconds", icrsProperties.getAi().getTimeoutSeconds(),
                "maxCompletionTokens", icrsProperties.getAi().getMaxCompletionTokens()));

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(normalizeBaseUrl(baseUrl))
                .modelName(modelName)
                .timeout(Duration.ofSeconds(Math.max(icrsProperties.getAi().getTimeoutSeconds(), 1)))
                .maxCompletionTokens(Math.max(icrsProperties.getAi().getMaxCompletionTokens(), 1))
                .build();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return null;
        if (url.endsWith("/v1") || url.endsWith("/v1/")) return url;
        return url.endsWith("/") ? url + "v1" : url + "/v1";
    }
}
