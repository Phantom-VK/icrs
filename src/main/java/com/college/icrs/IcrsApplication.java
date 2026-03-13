package com.college.icrs;

import com.college.icrs.bootstrap.SentimentServiceLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IcrsApplication {

	public static void main(String[] args) {
		SentimentServiceLauncher.startIfConfigured(args);
		SpringApplication.run(IcrsApplication.class, args);
	}
}
