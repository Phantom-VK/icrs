package com.college.icrs.bootstrap;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

record SentimentLauncherConfig(
        boolean aiEnabled,
        boolean sentimentEnabled,
        boolean autoStart,
        String baseUrl,
        String workingDirPath,
        int startupWaitMs
) {

    private static final String AI_ENABLED = "icrs.ai.enabled";
    private static final String SENTIMENT_ENABLED = "icrs.ai.sentiment.enabled";
    private static final String SENTIMENT_AUTO_START = "icrs.ai.sentiment.auto-start";
    private static final String SENTIMENT_BASE_URL = "icrs.ai.sentiment.base-url";
    private static final String SENTIMENT_WORKING_DIR = "icrs.ai.sentiment.working-dir";
    private static final String SENTIMENT_STARTUP_WAIT_MS = "icrs.ai.sentiment.startup-wait-ms";

    private static final boolean DEFAULT_AI_ENABLED = true;
    private static final boolean DEFAULT_SENTIMENT_ENABLED = true;
    private static final boolean DEFAULT_SENTIMENT_AUTO_START = true;
    private static final String DEFAULT_SENTIMENT_BASE_URL = "http://localhost:8090";
    private static final String DEFAULT_SENTIMENT_WORKING_DIR = "src/main/python/sentiment_service";
    private static final int DEFAULT_SENTIMENT_STARTUP_WAIT_MS = 15000;

    static SentimentLauncherConfig from(Properties properties, String[] args) {
        return new SentimentLauncherConfig(
                getBooleanProperty(properties, AI_ENABLED, args, DEFAULT_AI_ENABLED),
                getBooleanProperty(properties, SENTIMENT_ENABLED, args, DEFAULT_SENTIMENT_ENABLED),
                getBooleanProperty(properties, SENTIMENT_AUTO_START, args, DEFAULT_SENTIMENT_AUTO_START),
                getStringProperty(properties, SENTIMENT_BASE_URL, args, DEFAULT_SENTIMENT_BASE_URL),
                getStringProperty(properties, SENTIMENT_WORKING_DIR, args, DEFAULT_SENTIMENT_WORKING_DIR),
                getIntProperty(properties, SENTIMENT_STARTUP_WAIT_MS, args, DEFAULT_SENTIMENT_STARTUP_WAIT_MS)
        );
    }

    boolean shouldStart() {
        return aiEnabled && sentimentEnabled && autoStart;
    }

    private static String getStringProperty(Properties properties, String key, String[] args, String fallback) {
        String argValue = getCommandLineValue(args, key);
        if (StringUtils.hasText(argValue)) {
            return argValue;
        }

        String systemValue = System.getProperty(key);
        if (StringUtils.hasText(systemValue)) {
            return systemValue;
        }

        String propertyValue = properties.getProperty(key);
        if (StringUtils.hasText(propertyValue)) {
            return propertyValue;
        }

        return fallback;
    }

    private static boolean getBooleanProperty(Properties properties, String key, String[] args, boolean fallback) {
        String value = getStringProperty(properties, key, args, null);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value) : fallback;
    }

    private static int getIntProperty(Properties properties, String key, String[] args, int fallback) {
        String value = getStringProperty(properties, key, args, null);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String getCommandLineValue(String[] args, String key) {
        if (args == null || args.length == 0) {
            return null;
        }

        String prefix = "--" + key + "=";
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> arg.startsWith(prefix))
                .map(arg -> arg.substring(prefix.length()))
                .findFirst()
                .orElse(null);
    }
}
