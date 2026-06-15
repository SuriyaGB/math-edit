package com.gbrit.controller;

import com.gbrit.entity.Beneficiary;
import com.gbrit.service.BeneficiaryService;
import com.gbrit.utils.Constants;
import com.gbrit.utils.Util;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

@RestController
@RequestMapping("/beneficiary")
public class BeneficiaryController {

    private static final Logger logger = LoggerFactory.getLogger(BeneficiaryController.class);

    @Autowired
    BeneficiaryService beneficiaryService;

    /**
     * Add a new beneficiary.
     *
     * @param beneficiary The beneficiary to be added.
     * @return ResponseEntity indicating success or failure.
     */
    @SecurityRequirements({
            @SecurityRequirement(name = "Authorization")
    })
    @PostMapping("/addBeneficiary")
    public ResponseEntity<String> addBeneficiary(@RequestBody Beneficiary beneficiary,
                                                 @RequestHeader("userName") String userName,
                                                 @RequestHeader(name = "Authorization", required = false) String token,
                                                 @RequestHeader(name = "orgNameWithId", required = false) String orgId,
                                                 @RequestHeader(name = "orgName", required = false) String orgName) {
        beneficiary.setCreatedBy(userName);
        beneficiary.setCreatedDate(new Date());
        try {
            if (token.isEmpty() && orgId != null && orgName != null) {
                // If the organizationId is not present in claims or token is not present, use the orgId from headers
                beneficiaryService.addBeneficiary(beneficiary, Util.getCollectionNameWithOrgName(beneficiary, orgId), orgId, orgName);
                return ResponseEntity.ok(Constants.BENEFICIARY_NAME_SPACE + beneficiary.getUserName() + Constants.ADDED_SUCCESSFULLY);
            } else {
                Claims claims = Util.validateTokenAndGetClaims(token);
                // Extract organization ID from claims
                Long orgNameWithId = claims.get(Constants.ORG_ID, Long.class);
                String organizationName = claims.get(Constants.ORG_NAME, String.class);
                if (orgNameWithId != null) {
                    // Add beneficiary to the appropriate collection using organizationId from claims
                    beneficiaryService.addBeneficiary(beneficiary, Util.getCollectionNameWithOrgName(beneficiary, String.valueOf(orgNameWithId)), String.valueOf(orgNameWithId), organizationName);
                    return ResponseEntity.ok(Constants.BENEFICIARY_NAME_SPACE + beneficiary.getUserName() + Constants.ADDED_SUCCESSFULLY);
                }
            }
        }catch (Exception e) {
            // Handle other exceptions more gracefully
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
        // Handle the case where none of the conditions are met
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Constants.FAILED_TO_ADD_BENEFICIARY_SERVER);
    }

}


