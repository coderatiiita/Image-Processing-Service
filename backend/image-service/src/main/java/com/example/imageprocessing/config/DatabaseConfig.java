package com.example.imageprocessing.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "production")
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null) {
            throw new RuntimeException("DATABASE_URL environment variable is not set");
        }

        try {
            // Parse the DATABASE_URL
            java.net.URI uri = new java.net.URI(databaseUrl);
            
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort(); // Default PostgreSQL port
            String database = uri.getPath().substring(1); // Remove leading '/'
            String username = uri.getUserInfo().split(":")[0];
            String password = uri.getUserInfo().split(":")[1];
            
            // Build JDBC URL with proper format
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            
            // Optional: Set connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            return new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DATABASE_URL: " + e.getMessage(), e);
        }
    }
}