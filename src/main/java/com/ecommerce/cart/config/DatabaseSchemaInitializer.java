package com.ecommerce.cart.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.liquibase.default-schema:cart}")
    private String schema;

    public DatabaseSchemaInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void createSchema() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
    }
}
