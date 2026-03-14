package com.college.icrs.logging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LogFolderInitializer {

    private static final Logger log = LoggerFactory.getLogger(LogFolderInitializer.class);

    @PostConstruct
    public void ensureFolderExists() {
        Path logsDir = Path.of("logs");
        try {
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
                log.info("Created missing log directory at {}", logsDir.toAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Unable to ensure log directory {}: {}", logsDir, e.getMessage());
        }
    }
}
