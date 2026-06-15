package com.gbrit;

import com.gbrit.util.Constant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${mathEditorServiceUrl}")
    String MATH_EDITOR_SERVICE_URL;

    @Value("${userServiceUrl}")
    String USER_SERVICE_URL;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(Constant.USER, r -> r.path(Constant.USER_SERVICE)
                        .uri(USER_SERVICE_URL))
                .route(Constant.MATH_EDITOR_SERVICE, r -> r.path(Constant.MATH_SERVICE)
                        .uri(MATH_EDITOR_SERVICE_URL))
                .build();
    }
}