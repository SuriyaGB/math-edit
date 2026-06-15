package com.gbrit.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection="registration")
public class Registration {
    @Transient
    public static final String SEQUENCE_NAME = "registration_sequence";
    @Id
    private long registrationId;
    private String name;
    private String email;
    private String country;
    private String phone;
    private long organizationId;
    boolean enabled;
    private boolean isDeleted;
}
