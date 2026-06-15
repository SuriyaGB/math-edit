package com.gbrit.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {
    @Id
    private long id;
    private String userName;
    private String password;
    private String randomPassword;
    private long role;
    private String roleName;
    private String email;
    private boolean forcePassword;
    private String job;
    private long organizationId;
    private String organizationName;
    private String territory;
    private String reportingTo;
    private String reportingToName;
    private String startDate;
    private String endDate;
    private String userId;
    private boolean eligible;
    private String employeeType;
    private String notes;
    private boolean isError;
    private boolean isUploaded;
    private String description;
    private Map<String, Object> customFields = new HashMap<>();

    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;
    private Date deletedDate;
    private boolean isDeleted;

    public Beneficiary(long role, String job, String territory, String reportingTo, String startDate, String endDate, String userId, boolean eligible, String employeeType, String notes) {
        this.role = role;
        this.job = job;
        this.territory = territory;
        this.reportingTo = reportingTo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.eligible = eligible;
        this.employeeType = employeeType;
        this.notes = notes;
    }
}
