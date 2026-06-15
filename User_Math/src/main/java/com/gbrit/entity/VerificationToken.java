package com.gbrit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Calendar;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "Token")
public class VerificationToken {

    @Transient
    public static final String SEQUENCE_NAME = "Token_Sequence";
    @Id
    private long id;
    private String token;
    private Date expirationTime;
    private static final int EXPIRATION_TIME = 60 * 60;
    private long registrationId;
    private long organizationId;

    public VerificationToken(String token, Registration user) {
        super();
        this.token = token;
        this.expirationTime = this.getTokenExpirationTime();
    }

    public Date getTokenExpirationTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(new Date().getTime());
        calendar.add(Calendar.MINUTE, EXPIRATION_TIME);
        return new Date(calendar.getTime().getTime());
    }
}
