package com.skouna.reservator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

        @Bean
        OpenAPI customOpenAPI(
                        @Value("${api.title}") String title,
                        @Value("${api.version}") String version,
                        @Value("${api.description}") String description) {
                return new OpenAPI()
                                .info(new Info()
                                                .title(title)
                                                .version(version)
                                                .description(description));
        }
}
