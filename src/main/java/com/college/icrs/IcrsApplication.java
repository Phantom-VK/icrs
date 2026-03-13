package com.college.icrs;

import com.college.icrs.bootstrap.SentimentServiceLauncher;
import com.college.icrs.logging.IcrsLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@Slf4j
public class IcrsApplication {

	public static void main(String[] args) {
		log.info(IcrsLog.event("application.startup.begin", "application", "icrs"));
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
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(15);
		executor.initialize();
		log.info(IcrsLog.event("executor.initialized",
				"name", "aiTaskExecutor",
				"corePoolSize", 2,
				"maxPoolSize", 4,
				"queueCapacity", 50));
		return executor;
	}
}
