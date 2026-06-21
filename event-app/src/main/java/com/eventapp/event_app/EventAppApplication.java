package com.eventapp.event_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.eventapp")
@EntityScan("com.eventapp.model")
@EnableJpaRepositories("com.eventapp.repository")
public class EventAppApplication {

public static void main(String[] args) {
  SpringApplication.run(EventAppApplication.class, args);
	}
}

