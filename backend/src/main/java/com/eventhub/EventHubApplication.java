package com.eventhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point that bootstraps the Spring Boot application context.
 */
@SpringBootApplication
public class EventHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventHubApplication.class, args);
    }

}
