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

        // Transform postgresql:// to jdbc:postgresql://
        String jdbcUrl;
        if (databaseUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:" + databaseUrl;
        } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
            jdbcUrl = databaseUrl;
        } else {
            throw new RuntimeException("Unsupported database URL format: " + databaseUrl);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Optional: Set connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}