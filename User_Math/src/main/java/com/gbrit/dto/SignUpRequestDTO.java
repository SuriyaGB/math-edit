package com.gbrit.dto;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class SignUpRequestDTO {
    private long id;
    private String userName;
    private String password;
    private String beneficiary;
    private String currency;
    private long role;
    private String roleName;
    private String reportingToName;
    private String email;
    private boolean forcePassword;
    private String job;
    private String territory;
    private String reportingTo;
    private String startDate;
    private String endDate;
    private boolean eligible;
    private String employeeType;
    private String notes;
    private String oldPassword;
    private String beneficiaryId;
    private String organizationName;
    private boolean isError;
    private String description;
    private boolean isUploaded;
    private boolean isApproved;
    private Map<String, Object> customFields = new HashMap<>();

    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;
    private Date deletedDate;
    private boolean isDeleted;
}
