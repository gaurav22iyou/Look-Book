package com.example.bookingsystem.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor to automatically convert Render-style PostgreSQL connection strings
 * (e.g., postgresql://user:password@host:port/dbname) into valid JDBC URLs (jdbc:postgresql://host:port/dbname)
 * and extract credentials.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "renderDatabaseProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("DB_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("spring.datasource.url");
        }

        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            Map<String, Object> props = convertRenderUrl(rawUrl, environment);
            if (!props.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            }
        } else if (rawUrl != null && rawUrl.startsWith("jdbc:postgresql://")) {
            // Ensure spring.datasource.url is set if DB_URL was provided
            if (environment.getProperty("spring.datasource.url") == null || environment.getProperty("spring.datasource.url").isBlank()) {
                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", rawUrl);
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            }
        }
    }

    public static Map<String, Object> convertRenderUrl(String rawUrl, ConfigurableEnvironment environment) {
        Map<String, Object> props = new HashMap<>();
        try {
            String cleanUrl = rawUrl.trim();
            URI uri = URI.create(cleanUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath(); // contains leading '/'
            String userInfo = uri.getUserInfo();
            String query = uri.getQuery();

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append(path != null ? path : "");

            if (query != null && !query.isBlank()) {
                jdbcUrl.append("?").append(query);
            }

            props.put("spring.datasource.url", jdbcUrl.toString());

            // Extract credentials from userInfo if DB_USERNAME / DB_PASSWORD are not already defined
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                String currentUsername = environment != null ? environment.getProperty("DB_USERNAME") : null;
                String currentPassword = environment != null ? environment.getProperty("DB_PASSWORD") : null;

                if (currentUsername == null || currentUsername.isBlank()) {
                    props.put("spring.datasource.username", parts[0]);
                }
                if ((currentPassword == null || currentPassword.isBlank()) && parts.length > 1) {
                    props.put("spring.datasource.password", parts[1]);
                }
            }
        } catch (Exception e) {
            // Fallback: replace schema prefix directly
            String fallbackUrl;
            if (rawUrl.startsWith("postgres://")) {
                fallbackUrl = "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
            } else if (rawUrl.startsWith("postgresql://")) {
                fallbackUrl = "jdbc:postgresql://" + rawUrl.substring("postgresql://".length());
            } else {
                fallbackUrl = rawUrl;
            }
            props.put("spring.datasource.url", fallbackUrl);
        }
        return props;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
