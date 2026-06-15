package com.gbrit.dto;

import lombok.Data;

@Data
public class ValidationResponse {
    private String status;
    private String email;
    private String organizationId;
    private String userName;

    public ValidationResponse(String status, String email, String organizationId, String userName) {
        this.status = status;
        this.email = email;
        this.organizationId = organizationId;
        this.userName = userName;
    }
}

