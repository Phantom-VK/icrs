package com.college.icrs.ai.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class AiClient {

    @Value("${ai.apikey}")
    private String apiKey;

    @Value("${ai.baseurl}")
    private String baseUrl;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;



    @Bean
    public ChatModel chatModel() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API key missing. Set ai.apikey or DEEPSEEK_API_KEY/OPENAI_API_KEY.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("AI base URL missing. Set ai.baseurl or DEEPSEEK_API_BASE/OPENAI_API_BASE.");
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(normalizeBaseUrl(baseUrl))
                .modelName(modelName)
                .build();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return null;
        if (url.endsWith("/v1") || url.endsWith("/v1/")) return url;
        return url.endsWith("/") ? url + "v1" : url + "/v1";
    }
}
