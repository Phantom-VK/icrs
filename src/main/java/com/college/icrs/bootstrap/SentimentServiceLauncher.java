package com.college.icrs.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

public final class SentimentServiceLauncher {

    private static final Logger log = LoggerFactory.getLogger(SentimentServiceLauncher.class);

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
    private static final String DEFAULT_SENTIMENT_WORKING_DIR = "ml/sentiment_service";
    private static final int DEFAULT_SENTIMENT_STARTUP_WAIT_MS = 15000;

    private SentimentServiceLauncher() {
    }

    public static void startIfConfigured(String[] args) {
        Properties properties = loadApplicationProperties();

        boolean aiEnabled = getBooleanProperty(properties, AI_ENABLED, args, DEFAULT_AI_ENABLED);
        boolean sentimentEnabled = getBooleanProperty(properties, SENTIMENT_ENABLED, args, DEFAULT_SENTIMENT_ENABLED);
        boolean autoStart = getBooleanProperty(properties, SENTIMENT_AUTO_START, args, DEFAULT_SENTIMENT_AUTO_START);

        if (!aiEnabled || !sentimentEnabled || !autoStart) {
            return;
        }

        String baseUrl = getStringProperty(properties, SENTIMENT_BASE_URL, args, DEFAULT_SENTIMENT_BASE_URL);
        String workingDirPath = getStringProperty(properties, SENTIMENT_WORKING_DIR, args, DEFAULT_SENTIMENT_WORKING_DIR);
        int startupWaitMs = getIntProperty(properties, SENTIMENT_STARTUP_WAIT_MS, args, DEFAULT_SENTIMENT_STARTUP_WAIT_MS);

        if (isSentimentServiceHealthy(baseUrl, 1500)) {
            log.info("Sentiment service already running at {}", baseUrl);
            return;
        }

        File workingDir = new File(workingDirPath);
        if (!workingDir.exists() || !workingDir.isDirectory()) {
            log.warn("Sentiment service working directory not found: {}", workingDir.getAbsolutePath());
            return;
        }

        try {
            Process process = new ProcessBuilder("bash", "-lc", buildStartCommand(baseUrl))
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start();

            Thread outputThread = new Thread(() -> streamProcessOutput(process), "sentiment-service-stdout");
            outputThread.setDaemon(true);
            outputThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));

            if (waitForSentimentService(baseUrl, startupWaitMs)) {
                log.info("Sentiment service auto-started successfully at {}", baseUrl);
            } else {
                log.warn("Sentiment service process started but health check did not pass within {} ms", startupWaitMs);
            }
        } catch (Exception e) {
            log.warn("Failed to auto-start sentiment service: {}", e.getMessage());
        }
    }

    private static Properties loadApplicationProperties() {
        Properties properties = new Properties();
        try (var inputStream = SentimentServiceLauncher.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (Exception e) {
            log.debug("Unable to load application.properties for sentiment bootstrap: {}", e.getMessage());
        }
        return properties;
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

    private static String buildStartCommand(String baseUrl) {
        URI uri = URI.create(baseUrl);
        String host = StringUtils.hasText(uri.getHost()) ? uri.getHost() : "0.0.0.0";
        int port = uri.getPort() > 0 ? uri.getPort() : 8090;
        return """
                if [ -x .venv/bin/python ]; then
                  .venv/bin/python -m uvicorn app:app --host %s --port %d;
                else
                  python3 -m uvicorn app:app --host %s --port %d;
                fi
                """.formatted(host, port, host, port);
    }

    private static void streamProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[sentiment-service] {}", line);
            }
        } catch (Exception e) {
            log.debug("Stopped reading sentiment service output: {}", e.getMessage());
        }
    }

    private static boolean waitForSentimentService(String baseUrl, int timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 1000);
        while (System.currentTimeMillis() < deadline) {
            if (isSentimentServiceHealthy(baseUrl, 1000)) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static boolean isSentimentServiceHealthy(String baseUrl, int timeoutMs) {
        if (!StringUtils.hasText(baseUrl)) {
            return false;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl.replaceAll("/+$", "") + "/health")
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.connect();
            int status = connection.getResponseCode();
            connection.disconnect();
            return status >= 200 && status < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
