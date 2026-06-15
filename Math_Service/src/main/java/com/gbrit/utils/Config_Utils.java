package com.gbrit.utils;

public class Config_Utils {
    public static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    public static final String[] REQUEST_MATCHERS = {
            "/formulas/**",
            "/beneficiary/**",
            "/baseControl/**",
            "/health"
    };
}
