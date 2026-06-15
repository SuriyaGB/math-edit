package com.gbrit.security;

import com.gbrit.dto.RequestMeta;
import com.gbrit.entity.Organization;
import com.gbrit.util.JwtUtils;
import com.gbrit.util.MessageConstants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RequestMeta requestMeta;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String auth = request.getHeader(MessageConstants.AUTHORIZATION_HEADER);

        if (!(request.getRequestURI().contains(MessageConstants.LOGIN) || request.getRequestURI().contains(MessageConstants.SIGNUP))) {
            try {
                // Verify the JWT token and extract claims
                Claims claims = jwtUtils.verify(auth);
                if (claims != null) {
                    requestMeta.setId(claims.get(MessageConstants.ID,Long.class));
                    requestMeta.setUserName(claims.get(MessageConstants.USER_NAME, String.class));
                    requestMeta.setPassword(claims.get(MessageConstants.PASSWORD, String.class));
                    requestMeta.setForcePassword(claims.get(MessageConstants.FORCE_PASSWORD,Boolean.class));
                    requestMeta.setBeneficiary(claims.get(MessageConstants.BENEFICIARY, String.class));
                    requestMeta.setRole(claims.get(MessageConstants.ROLE, Long.class));
                    requestMeta.setRoleName(claims.get(MessageConstants.ROLE_NAME, String.class));
                    requestMeta.setReportingToName(claims.get(MessageConstants.REPORTING_TO_NAME, String.class));
                    requestMeta.setEmail(claims.get(MessageConstants.EMAIL, String.class));
                    requestMeta.setOrganizationId(claims.get(MessageConstants.ORG_ID, Long.class));
                    requestMeta.setOrganizationName(claims.get(MessageConstants.ORG_NAME, String.class));
                    requestMeta.setBeneficiaryId(claims.get(MessageConstants.BENEFICIARY_ID, String.class));
                    requestMeta.setJob(claims.get(MessageConstants.JOB, String.class));
                    requestMeta.setTerritory(claims.get(MessageConstants.TERRITORY, String.class));
                    requestMeta.setReportingTo(claims.get(MessageConstants.REPORTING_TO, String.class));
                    requestMeta.setStartDate(claims.get(MessageConstants.START_DATE, String.class));
                    requestMeta.setEndDate(claims.get(MessageConstants.END_DATE, String.class));
                    requestMeta.setEligible(claims.get(MessageConstants.ELIGIBLE, Boolean.class));
                    requestMeta.setEmployeeType(claims.get(MessageConstants.EMPLOYEE_TYPE, String.class));
                    requestMeta.setNotes(claims.get(MessageConstants.NOTES, String.class));
                    requestMeta.setApproved(claims.get(MessageConstants.APPROVAL, Boolean.class));
                    requestMeta.setMonitoredUser(claims.get(MessageConstants.IS_MONITORED_USER, Boolean.class));
                }
                else {
                    // Handle invalid token case
                    // You might want to send an appropriate response or redirect
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, MessageConstants.INVALID_TOKEN);
                    return false;
                }
            } catch (Exception e) {
                // Handle token verification exception
                // You might want to send an appropriate response or redirect
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, MessageConstants.TOKEN_VERIFICATION_FAILED);
                return false;
            }
        }

        // Call the super.preHandle to continue handling the request
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
