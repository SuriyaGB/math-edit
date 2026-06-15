package com.gbrit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "RefreshToken")
public class RefreshToken {
    @Transient
    public static final String SEQUENCE_NAME = "refreshToken_sequence";
    @Id
    private long id;
    private String token;
    private User user;
}

