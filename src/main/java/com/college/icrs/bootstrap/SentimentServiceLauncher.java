package com.college.icrs.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public final class SentimentServiceLauncher {

    private static final Logger log = LoggerFactory.getLogger(SentimentServiceLauncher.class);

    private SentimentServiceLauncher() {
    }

    public static void startIfConfigured(String[] args) {
        SentimentLauncherConfig config = SentimentLauncherConfig.from(loadApplicationProperties(), args);
        if (!config.shouldStart()) {
            return;
        }

        if (SentimentLauncherSupport.isSentimentServiceHealthy(config.baseUrl(), 1500)) {
            log.info("Sentiment service already running at {}", config.baseUrl());
            return;
        }

        File workingDir = SentimentLauncherSupport.resolveWorkingDir(config.workingDirPath());
        if (!workingDir.exists() || !workingDir.isDirectory()) {
            log.warn("Sentiment service working directory not found: {}", workingDir.getAbsolutePath());
            return;
        }

        try {
            Process process = new ProcessBuilder("bash", "-lc", SentimentLauncherSupport.buildStartCommand(config.baseUrl()))
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start();

            Thread outputThread = new Thread(() -> SentimentLauncherSupport.streamProcessOutput(process, log), "sentiment-service-stdout");
            outputThread.setDaemon(true);
            outputThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));

            if (SentimentLauncherSupport.waitForSentimentService(config.baseUrl(), config.startupWaitMs())) {
                log.info("Sentiment service auto-started successfully at {}", config.baseUrl());
            } else {
                log.warn("Sentiment service process started but health check did not pass within {} ms", config.startupWaitMs());
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
}
