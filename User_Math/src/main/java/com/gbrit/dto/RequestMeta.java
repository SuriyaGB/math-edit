package com.gbrit.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
public class RequestMeta {
    private long id;
    private String userName;
    private String password;
    private String beneficiary;
    private long role;
    private String reportingToName;
    private String roleName;
    private String email;
    private Long organizationId;
    private String organizationName;
    private String beneficiaryId;
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
    private boolean isError;
    private boolean isUploaded;
    private String description;
    private boolean isApproved;
    private boolean isMonitoredUser;
    private Map<String, Object> customFields = new HashMap<>();
}
