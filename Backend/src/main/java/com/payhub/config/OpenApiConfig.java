package com.payhub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata. The Angular frontend relies on the generated
 * {@code /v3/api-docs} contract, so the docs must stay complete and accurate.
 * Swagger UI is served at {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payhubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayHub API")
                        .description("SaaS Subscription Billing Platform - REST API (modular monolith)")
                        .version("v1")
                        .contact(new Contact().name("PayHub"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT issued after Google login.")));
    }
}
