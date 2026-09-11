package com.ecommerce.cart.config;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseConfig {

    @Value("${spring.liquibase.default-schema:cart}")
    private String schema;

    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase() {
            @Override
            public void afterPropertiesSet() {
                try {
                    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
                    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
                    super.afterPropertiesSet();
                } catch (liquibase.exception.LiquibaseException e) {
                    throw new IllegalStateException("Failed to initialize Liquibase", e);
                }
            }
        };
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setDefaultSchema(schema);
        liquibase.setContexts(properties.getContexts() != null ? String.join(",", properties.getContexts()) : "");
        liquibase.setDropFirst(properties.isDropFirst());
        return liquibase;
    }
}
