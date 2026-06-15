package com.gbrit;

import com.gbrit.util.Constant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Value("${frontEndUrl}")
    String FRONT_END_URL;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Collections.singletonList(FRONT_END_URL));
        corsConfig.setAllowedMethods(Arrays.asList(Constant.ALLOWED_METHODS));
        corsConfig.setAllowedHeaders(Arrays.asList(Constant.ALLOWED_HEADERS));
        corsConfig.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(Constant.PATH, corsConfig);
        return new CorsWebFilter(source);
    }
}