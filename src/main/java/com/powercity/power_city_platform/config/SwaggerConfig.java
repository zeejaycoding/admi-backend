package com.powercity.power_city_platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Power City Platform API")
                        .version("1.0.0")
                        .description("API documentation for Power City International Platform")
                        .contact(new Contact()
                                .name("Power City International")
                                .email("support@powercityinternational.org")))
                        .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                                        .url("http://localhost:3000")
                                        .description("Development server"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token without 'Bearer ' prefix")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
            return GroupedOpenApi.builder()
                            .group("power-city-platform")
                            .pathsToMatch("/api/v1/**")
                            .build();
    }
}