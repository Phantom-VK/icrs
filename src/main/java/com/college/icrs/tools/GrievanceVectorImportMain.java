package com.college.icrs.tools;

import com.college.icrs.IcrsApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;

public final class GrievanceVectorImportMain {

    private GrievanceVectorImportMain() {
    }

    public static void main(String[] args) {
        String importFile = System.getProperty("grievanceImportFile");
        if (importFile == null || importFile.isBlank()) {
            throw new IllegalArgumentException("Missing system property: grievanceImportFile");
        }

        boolean replaceExisting = Boolean.parseBoolean(
                System.getProperty("grievanceImportReplaceExisting", "false")
        );

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(IcrsApplication.class)
                .properties(
                        "spring.main.banner-mode=off",
                        "server.port=0",
                        "icrs.ai.sentiment.auto-start=false"
                )
                .run(args)) {
            GrievanceVectorImportService importer = context.getBean(GrievanceVectorImportService.class);
            importer.importFile(Path.of(importFile), replaceExisting);
            SpringApplication.exit(context);
        }
    }
}
