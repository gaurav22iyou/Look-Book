package com.example.bookingsystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class BookingSystemApplication {

    private static final Logger log = LoggerFactory.getLogger(BookingSystemApplication.class);
    private final Environment env;

    public BookingSystemApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(BookingSystemApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String appName = env.getProperty("spring.application.name", "booknow");
        String[] activeProfiles = env.getActiveProfiles();
        String profiles = activeProfiles.length > 0 ? Arrays.toString(activeProfiles) : "[default]";
        String rawDbUrl = env.getProperty("spring.datasource.url", "Not Configured");
        String sanitizedDbUrl = sanitizeJdbcUrl(rawDbUrl);
        String serverPort = env.getProperty("server.port", "8080");

        log.info("=================================================================");
        log.info("Application '{}' is running!", appName);
        log.info("Active Profile(s): {}", profiles);
        log.info("Database URL:      {}", sanitizedDbUrl);
        log.info("Server Port:       {}", serverPort);
        log.info("=================================================================");
    }

    private String sanitizeJdbcUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^/@:]+@", ":***@");
    }
}
