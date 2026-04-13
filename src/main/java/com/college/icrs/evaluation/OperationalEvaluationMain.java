package com.college.icrs.evaluation;

import com.college.icrs.IcrsApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class OperationalEvaluationMain {

    private OperationalEvaluationMain() {
    }

    public static void main(String[] args) throws Exception {
        OperationalEvaluationConfig config = OperationalEvaluationConfig.fromSystemProperties();

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(IcrsApplication.class)
                .properties(
                        "spring.main.banner-mode=off",
                        "server.port=0",
                        "icrs.ai.sentiment.auto-start=false"
                )
                .run(args)) {
            OperationalEvaluationService service = context.getBean(OperationalEvaluationService.class);
            OperationalEvaluationReport report = service.run(config);
            System.out.printf(
                    "Operational evaluation finished. Variant=%s, Results=%d, outputDir=%s%n",
                    config.experimentVariant(),
                    report.results().size(),
                    config.outputDir().toAbsolutePath()
            );
            SpringApplication.exit(context);
        }
    }
}
