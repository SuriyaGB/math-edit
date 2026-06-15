package com.gbrit.util;

public class Constant {
    public static final String[] USER_SERVICE = {
            "/user/**",
            "/register/**"
    };
    public static final String[] MATH_SERVICE = {
            "/formulas/**",
            "/beneficiary/**",
            "/baseControl/**"
    };
    public static final String USER = "user-service";
    public static final String MATH_EDITOR_SERVICE = "MATH-EDITOR-SERVICE";
    public static final String[] ALLOWED_METHODS = {
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "UPDATE"
    };

    public static final String[] ALLOWED_HEADERS = {
            "authorization",
            "Content-Type",
            "Origin",
            "Accept",
            "userName",
            "id",
            "orgNameWithId",
            "orgId",
            "pojoType",
            "fileType",
            "entityName",
            "token",
            "domain",
            "orgName",
            "url",
            "userEmail",
            "beneficiaryName",
            "beneficiaryId",
            "registration",
            "file",
            "year",
            "period",
            "fiscalYear",
            "oldPassword",
            "currentPassword",
            "userId",
            "currency",
            "type",
            "isgooglelogin"
    };
    public static final String PATH = "/**";
}
