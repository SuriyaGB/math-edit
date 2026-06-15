package com.gbrit.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Document(collection = "User")
public class User implements Serializable {
    @Transient
    public static final String SEQUENCE_NAME = "users_sequence";
    @Id
    private long id;
    private String userName;
    private String password;
    private String beneficiary;
    private long role;
    private String roleName;
    private String email;
    private boolean forcePassword;
    private String job;
    private String territory;
    private String reportingTo;
    private String reportingToName;
    private String startDate;
    private String endDate;
    private boolean eligible;
    private String employeeType;
    private String notes;
    private String resetToken;
    private Map<String, Object> customFields = new HashMap<>();
    private long organizationId;
    private String organizationName;
    private String beneficiaryId;
    private boolean isError;
    private boolean isUploaded;
    private String description;
    private boolean isApproved;
    private boolean isMonitoredUser;
    private long apiCallCount;

    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;
    private Date deletedDate;
    private boolean isDeleted;

    public User(String userName, String password, String beneficiary) {
        this.userName = userName;
        this.password = password;
        this.beneficiary = beneficiary;
    }

    public User() {
    }
}
