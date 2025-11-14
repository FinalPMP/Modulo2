package com.retrocore.mod2.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator loginRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("login-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://login-service:8080"))
                .build();
    }
}
