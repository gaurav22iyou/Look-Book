package com.example.bookingsystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsRenderPostgresUrlWithCredentials() {
        String renderUrl = "postgresql://myuser:secret123@dpg-c1234.oregon-postgres.render.com:5432/booknow_prod";
        MockEnvironment env = new MockEnvironment();

        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.convertRenderUrl(renderUrl, env);

        assertEquals("jdbc:postgresql://dpg-c1234.oregon-postgres.render.com:5432/booknow_prod", props.get("spring.datasource.url"));
        assertEquals("myuser", props.get("spring.datasource.username"));
        assertEquals("secret123", props.get("spring.datasource.password"));
    }

    @Test
    void convertsRenderPostgresSchemeWithQueryParameters() {
        String renderUrl = "postgres://usr:pwd@internal-db:5432/mydb?sslmode=require";
        MockEnvironment env = new MockEnvironment();

        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.convertRenderUrl(renderUrl, env);

        assertEquals("jdbc:postgresql://internal-db:5432/mydb?sslmode=require", props.get("spring.datasource.url"));
        assertEquals("usr", props.get("spring.datasource.username"));
        assertEquals("pwd", props.get("spring.datasource.password"));
    }

    @Test
    void postProcessesEnvironment_WithDB_URL() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DB_URL", "postgresql://dbuser:dbpass@render-host:5432/booknow_db");

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, null);

        assertTrue(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
        assertEquals("jdbc:postgresql://render-host:5432/booknow_db", env.getProperty("spring.datasource.url"));
        assertEquals("dbuser", env.getProperty("spring.datasource.username"));
        assertEquals("dbpass", env.getProperty("spring.datasource.password"));
    }

    @Test
    void postProcessesEnvironment_WithJdbcUrl_DoesNotAlterIt() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/booknow_db");

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, null);

        assertEquals("jdbc:postgresql://localhost:5432/booknow_db", env.getProperty("spring.datasource.url"));
    }
}
