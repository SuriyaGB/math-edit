package com.gbrit.util;

import com.gbrit.entity.User;
import com.gbrit.exception.AccessDeniedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtils {

    /**
     * Generate a JWT token for the provided user.
     *
     * @param user The user entity for which to generate the token.
     * @return The generated JWT token as a string.
     */
    public String generateJwt(User user){
        // Calculate the expiration time for the token
        long milliTime = System.currentTimeMillis();
//        long expiryTime = milliTime + MessageConstants.EXPIRY_DURATION * 1000;
        Date issuedAt = new Date(milliTime);
//        Date expiryAt = new Date(expiryTime);
        // Create claims for the token
        Claims claims = Jwts.claims()
                .setIssuer(String.valueOf(user.getId())) // Set issuer as user's ID
                .setIssuedAt(issuedAt);// Set issuance time
//                .setExpiration(expiryAt); // Set expiration time
        // Add additional user-related data to the claims
        claims.put(MessageConstants.ID,user.getId());
        claims.put(MessageConstants.USER_NAME, user.getUserName());
        claims.put(MessageConstants.PASSWORD, user.getPassword());
        claims.put(MessageConstants.FORCE_PASSWORD, user.isForcePassword());
        claims.put(MessageConstants.BENEFICIARY, user.getBeneficiary());
        claims.put(MessageConstants.ROLE, user.getRole());
        claims.put(MessageConstants.ROLE_NAME, user.getRoleName());
        claims.put(MessageConstants.REPORTING_TO_NAME, user.getReportingToName());
        claims.put(MessageConstants.EMAIL, user.getEmail());
        claims.put(MessageConstants.ORG_ID, user.getOrganizationId());
        claims.put(MessageConstants.ORG_NAME, user.getOrganizationName());
        claims.put(MessageConstants.BENEFICIARY_ID, user.getBeneficiaryId());
        claims.put(MessageConstants.JOB, user.getJob());
        claims.put(MessageConstants.TERRITORY, user.getTerritory());
        claims.put(MessageConstants.REPORTING_TO, user.getReportingTo());
        claims.put(MessageConstants.START_DATE, user.getStartDate());
        claims.put(MessageConstants.END_DATE, user.getEndDate());
        claims.put(MessageConstants.ELIGIBLE, user.isEligible());
        claims.put(MessageConstants.EMPLOYEE_TYPE, user.getEmployeeType());
        claims.put(MessageConstants.NOTES, user.getNotes());
        claims.put(MessageConstants.APPROVAL, user.isApproved());
        claims.put(MessageConstants.IS_MONITORED_USER, user.isMonitoredUser());
        // Build and sign the JWT token
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, MessageConstants.SECRET_KEY) // Sign with HS512 algorithm and secret key
                .compact();
    }

    /**
     * Verify and decode a JWT token.
     *
     * @param authorization The JWT token string to verify.
     * @return The claims extracted from the token.
     * @throws AccessDeniedException if verification fails.
     */
    public Claims verify(String authorization) {
        try {
            // Parse and verify the JWT token using the secret key
            return Jwts.parser().setSigningKey(MessageConstants.SECRET_KEY).parseClaimsJws(authorization).getBody();
        } catch(Exception e) {
            // If verification fails, throw an AccessDeniedException
            throw new AccessDeniedException(MessageConstants.ACCESS_DENIED);
        }
    }
}
