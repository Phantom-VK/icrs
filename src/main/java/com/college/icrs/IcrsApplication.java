package com.college.icrs;

import com.college.icrs.bootstrap.SentimentServiceLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class IcrsApplication {

	public static void main(String[] args) {
		SentimentServiceLauncher.startIfConfigured(args);
		SpringApplication.run(IcrsApplication.class, args);
	}

	@Bean(name = "aiTaskExecutor")
	public Executor aiTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("ai-worker-");
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.initialize();
		return executor;
	}
}
