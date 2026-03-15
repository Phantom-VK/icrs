package com.college.icrs.bootstrap;

import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

final class SentimentLauncherSupport {

    private SentimentLauncherSupport() {
    }

    static File resolveWorkingDir(String workingDirPath) {
        return new File(workingDirPath);
    }

    static String buildStartCommand(String baseUrl) {
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

    static void streamProcessOutput(Process process, Logger log) {
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

    static boolean waitForSentimentService(String baseUrl, int timeoutMs) {
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

    static boolean isSentimentServiceHealthy(String baseUrl, int timeoutMs) {
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
