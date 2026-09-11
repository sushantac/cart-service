package com.ecommerce.cart.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LiquibaseConfig {

    @Value("${spring.liquibase.default-schema:cart}")
    private String schema;

    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase() {
            @Override
            public void afterPropertiesSet() {
                // Create schema before Liquibase initializes
                org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
                jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
                super.afterPropertiesSet();
            }
        };
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setDefaultSchema(schema);
        liquibase.setContexts(properties.getContexts());
        liquibase.setLabels(properties.getLabels());
        liquibase.setDropFirst(properties.isDropFirst());
        return liquibase;
    }
}
