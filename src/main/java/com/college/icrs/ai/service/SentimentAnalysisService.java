package com.college.icrs.ai.service;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.model.Sentiment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class SentimentAnalysisService {

    private final ObjectMapper objectMapper;
    private final IcrsProperties icrsProperties;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public SentimentDecision analyze(String text) {
        IcrsProperties.Sentiment cfg = icrsProperties.getAi().getSentiment();
        if (!cfg.isEnabled() || !StringUtils.hasText(text)) {
            return SentimentDecision.unavailable();
        }

        try {
            String baseUrl = cfg.getBaseUrl().replaceAll("/+$", "");
            String payload = objectMapper.writeValueAsString(new SentimentRequest(text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/analyze"))
                    .timeout(Duration.ofMillis(Math.max(cfg.getTimeoutMs(), 500)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Sentiment service returned non-2xx status {} body={}", response.statusCode(), response.body());
                return SentimentDecision.unavailable();
            }

            SentimentResponse api = objectMapper.readValue(response.body(), SentimentResponse.class);
            if (api == null || !StringUtils.hasText(api.getLabel())) {
                return SentimentDecision.unavailable();
            }

            Double score = clamp(api.getScore());
            Sentiment sentiment = mapSentiment(api.getLabel(), score);
            return new SentimentDecision(sentiment, score, StringUtils.hasText(api.getModel()) ? api.getModel() : cfg.getModelName());

        } catch (Exception e) {
            log.warn("Sentiment analysis service failed: {}", e.getMessage());
            return SentimentDecision.unavailable();
        }
    }

    private Sentiment mapSentiment(String label, Double score) {
        String normalized = label.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

        if ("VERY_NEGATIVE".equals(normalized)) {
            return Sentiment.VERY_NEGATIVE;
        }
        if ("NEUTRAL".equals(normalized)) {
            return Sentiment.NEUTRAL;
        }
        if ("NEGATIVE".equals(normalized)) {
            if (score != null && score >= icrsProperties.getAi().getSentiment().getVeryNegativeThreshold()) {
                return Sentiment.VERY_NEGATIVE;
            }
            if (score != null && score < icrsProperties.getAi().getSentiment().getNeutralBandUpper()) {
                return Sentiment.NEUTRAL;
            }
            return Sentiment.NEGATIVE;
        }
        if ("POSITIVE".equals(normalized)) {
            if (score != null && score < icrsProperties.getAi().getSentiment().getNeutralBandUpper()) {
                return Sentiment.NEUTRAL;
            }
            return Sentiment.POSITIVE;
        }

        return null;
    }

    private Double clamp(Double value) {
        if (value == null) return null;
        if (value < 0d) return 0d;
        if (value > 1d) return 1d;
        return value;
    }

    private record SentimentRequest(String text) {}

    @Getter
    @Setter
    private static class SentimentResponse {
        private String label;
        private Double score;
        private String model;
    }

    public record SentimentDecision(Sentiment sentiment, Double confidence, String modelName) {
        public static SentimentDecision unavailable() {
            return new SentimentDecision(null, null, null);
        }
    }
}
