package com.gbrit.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Builder
@Data
@AllArgsConstructor
public class APIResponse {
    private Integer status;
    private String accessToken;
    private String token;
    private String error;
    private JsonNode userRoleAccess;
    private String errorDetails; // New field for detailed error information

    public APIResponse() {
        this.status = HttpStatus.OK.value();
    }
}
