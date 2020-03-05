package org.activiti.cloud.starter.query.configuration;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import(QuerySwaggerConfig.class)
public class ActivitiQueryAutoConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.query.liquibase")
    public LiquibaseProperties queryLiquibaseProperties() {
        return new LiquibaseProperties();
    }

}
