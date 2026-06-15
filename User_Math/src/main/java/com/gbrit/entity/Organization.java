package com.gbrit.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "Organization")
public class Organization {
    @Transient
    public static final String SEQUENCE_NAME = "Organization_Sequence";
    @Id
    private long organizationId;
    private String organizationName;
    private byte[] imageData;
    private String currency;
    private Date createdDate;
}
