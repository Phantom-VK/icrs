package com.college.icrs.ai.service;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
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
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class SentimentAnalysisService {

    private final ObjectMapper objectMapper;
    private final IcrsProperties icrsProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .build();

    public SentimentDecision analyze(String text) {
        IcrsProperties.Sentiment cfg = icrsProperties.getAi().getSentiment();
        if (!cfg.isEnabled() || !StringUtils.hasText(text)) {
            log.info(IcrsLog.event("sentiment.analysis.skipped",
                    "enabled", cfg.isEnabled(),
                    "hasText", StringUtils.hasText(text)));
            return SentimentDecision.unavailable();
        }

        try {
            log.info(IcrsLog.event("sentiment.analysis.start", "baseUrl", cfg.getBaseUrl(), "model", cfg.getModelName()));
            String baseUrl = cfg.getBaseUrl().replaceAll("/+$", "");
            String payload = objectMapper.writeValueAsString(new SentimentRequest(text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/analyze"))
                    .timeout(Duration.ofMillis(Math.max(cfg.getTimeoutMs(), 500)))
                    .version(Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(IcrsLog.event("sentiment.analysis.failed",
                        "statusCode", response.statusCode(),
                        "body", truncate(response.body(), 200),
                        "reason", "non-2xx"));
                return SentimentDecision.unavailable();
            }

            SentimentResponse api = objectMapper.readValue(response.body(), SentimentResponse.class);
            if (api == null || !StringUtils.hasText(api.getLabel())) {
                return SentimentDecision.unavailable();
            }

            Double score = clamp(api.getScore());
            Sentiment sentiment = mapSentiment(api.getLabel(), score);
            log.info(IcrsLog.event("sentiment.analysis.completed",
                    "sentiment", sentiment,
                    "score", score,
                    "model", StringUtils.hasText(api.getModel()) ? api.getModel() : cfg.getModelName()));
            return new SentimentDecision(sentiment, score, StringUtils.hasText(api.getModel()) ? api.getModel() : cfg.getModelName());

        } catch (Exception e) {
            log.warn(IcrsLog.event("sentiment.analysis.failed", "reason", e.getClass().getSimpleName()), e);
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

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
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
