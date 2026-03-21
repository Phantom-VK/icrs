package com.college.icrs.ai.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrievanceDecisionAiConfiguration {

    @Bean
    public GrievanceClassifierAiService grievanceClassifierAiService(@Qualifier("decisionChatModel") ChatModel chatModel) {
        return AiServices.builder(GrievanceClassifierAiService.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public GrievanceResolverAiService grievanceResolverAiService(@Qualifier("decisionChatModel") ChatModel chatModel) {
        return AiServices.builder(GrievanceResolverAiService.class)
                .chatModel(chatModel)
                .build();
    }
}
