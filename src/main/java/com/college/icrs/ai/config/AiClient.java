package com.college.icrs.ai.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class AiClient {

    @Value("${deepseekapi.key}")
    private String deepseekAPI;

    @Value("${deepseek.baseurl}")
    private String baseUrl;

    @Bean
    ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(deepseekAPI)
                .baseUrl(baseUrl)
                .modelName("deepseek-chat")
                .build();
    }
}
