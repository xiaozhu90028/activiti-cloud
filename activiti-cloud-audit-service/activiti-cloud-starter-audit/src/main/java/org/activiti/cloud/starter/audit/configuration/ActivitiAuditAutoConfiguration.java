package org.activiti.cloud.starter.audit.configuration;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import(SwaggerConfig.class)
public class ActivitiAuditAutoConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.audit.liquibase")
    public LiquibaseProperties auditLiquibaseProperties() {
        return new LiquibaseProperties();
    }

}
