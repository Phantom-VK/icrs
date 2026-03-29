package com.college.icrs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@ConfigurationProperties(prefix = "icrs")
public class IcrsProperties {

    private final Cors cors = new Cors();
    private final Files files = new Files();
    private final Ai ai = new Ai();

    @Setter
    @Getter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

    }

    @Setter
    @Getter
    public static class Files {
        private List<String> allowedMimeTypes = new ArrayList<>();

    }

    @Setter
    @Getter
    public static class Ai {
        private boolean enabled = true;
        private double autoResolveConfidenceThreshold = 0.80d;
        private int maxDescriptionChars = 4000;
        private int timeoutSeconds = 90;
        private int maxCompletionTokens = 400;
        private String systemUserEmail = "ai.system@icrs.local";
        private String decisionSource = "DEEPSEEK_AGENTIC_V1";
        private final Rag rag = new Rag();
        private final Sentiment sentiment = new Sentiment();

        @Setter
        @Getter
        public static class Rag {
            private boolean enabled = true;
            private int topK = 3;
        }
    }

    @Setter
    @Getter
    public static class Sentiment {
        private boolean enabled = true;
        private boolean autoStart = true;
        private String baseUrl = "http://localhost:8090";
        private int timeoutMs = 3000;
        private double neutralBandUpper = 0.60d;
        private String modelName = "siebert/sentiment-roberta-large-english";
        private String workingDir = "src/main/python/sentiment_service";
        private int startupWaitMs = 15000;
    }
}
