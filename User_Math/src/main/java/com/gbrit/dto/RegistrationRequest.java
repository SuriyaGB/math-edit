package com.gbrit.dto;

public record RegistrationRequest(
        long id,
        String name,
        String email,
        String organizationName,
        String phone,
        String country){
}
