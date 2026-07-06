package com.focisolutions.takehomeexercise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI todoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Todo REST API")
                .description("A RESTful JSON API for managing personal to-do items.")
                .version("0.0.1"));
    }
}
