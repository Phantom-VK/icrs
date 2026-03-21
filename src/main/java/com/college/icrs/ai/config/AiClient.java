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

    private static final int PLANNER_MAX_TOKENS = 160;
    private static final int DECISION_MAX_TOKENS = 220;

    @Value("${ai.apikey}")
    private String apiKey;

    @Value("${ai.baseurl}")
    private String baseUrl;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;

    @Value("${ai.temperature:0.0}")
    private Double temperature;

    private final IcrsProperties icrsProperties;

    @Bean("plannerChatModel")
    public ChatModel plannerChatModel() {
        validateConfiguration();
        log.info(IcrsLog.event("ai.chat-model.initialized",
                "role", "planner",
                "modelName", modelName,
                "baseUrl", normalizeBaseUrl(baseUrl),
                "timeoutSeconds", icrsProperties.getAi().getTimeoutSeconds(),
                "maxCompletionTokens", PLANNER_MAX_TOKENS,
                "temperature", temperature,
                "maxRetries", 2));

        return baseBuilder(normalizeBaseUrl(baseUrl))
                .temperature(temperature)
                .maxCompletionTokens(PLANNER_MAX_TOKENS)
                .maxTokens(PLANNER_MAX_TOKENS)
                .maxRetries(2)
                .build();
    }

    @Bean("decisionChatModel")
    public ChatModel decisionChatModel() {
        validateConfiguration();
        log.info(IcrsLog.event("ai.chat-model.initialized",
                "role", "decision",
                "modelName", modelName,
                "baseUrl", normalizeBaseUrl(baseUrl),
                "timeoutSeconds", icrsProperties.getAi().getTimeoutSeconds(),
                "maxCompletionTokens", DECISION_MAX_TOKENS,
                "temperature", 0.0,
                "maxRetries", 2,
                "responseFormat", "json_object"));

        return baseBuilder(normalizeBaseUrl(baseUrl))
                .temperature(0.0d)
                .maxCompletionTokens(DECISION_MAX_TOKENS)
                .maxTokens(DECISION_MAX_TOKENS)
                .responseFormat("json_object")
                .maxRetries(2)
                .build();
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API key missing. Set ai.apikey or DEEPSEEK_API_KEY/OPENAI_API_KEY.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("AI base URL missing. Set ai.baseurl or DEEPSEEK_API_BASE/OPENAI_API_BASE.");
        }
    }

    private OpenAiChatModel.OpenAiChatModelBuilder baseBuilder(String normalizedBaseUrl) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(normalizedBaseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(Math.max(icrsProperties.getAi().getTimeoutSeconds(), 1)))
                .maxCompletionTokens(Math.max(icrsProperties.getAi().getMaxCompletionTokens(), 1));
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return null;
        if (url.endsWith("/v1") || url.endsWith("/v1/")) return url;
        return url.endsWith("/") ? url + "v1" : url + "/v1";
    }
}
