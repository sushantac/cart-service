package com.ecommerce.cart.config;

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
                org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
                jdbcTemplate.execute("CREATE cart IF NOT EXISTS " + schema);
                super.afterPropertiesSet();
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
