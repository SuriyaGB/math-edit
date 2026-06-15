package com.gbrit.controller;

import com.gbrit.entity.Formula;
import com.gbrit.exception.CollectionNotFoundException;
import com.gbrit.exception.FormulaNotFoundException;
import com.gbrit.service.FormulaService;
import com.gbrit.utils.Constants;
import com.gbrit.utils.Util;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/formulas")
public class FormulaController {

    @Autowired
    FormulaService formulaService;

    private static final Logger logger = LoggerFactory.getLogger(FormulaController.class);

    /**
     * This method handles the addition of a beneficiary to a product.
     *
     * @param formula The product object containing beneficiary details.
     * @param token   The authorization token in the request header.
     * @return ResponseEntity<String> indicating the result of the beneficiary addition.
     */
    @PostMapping("/addFormulas")
    @SecurityRequirements({
            @SecurityRequirement(name = "Authorization") // This should match the security scheme defined in your OpenAPI definition
    })
    public ResponseEntity<String> addFormulas(@RequestBody Formula formula,
                                             @RequestHeader("Authorization") String token) {
        long startTimeMillis = System.currentTimeMillis();
        try {
            Claims claims = Util.validateTokenAndGetClaims(token);
            if (claims != null) {
                String userName = claims.get(Constants.USER_NAME, String.class);
                Long orgNameWithId = claims.get(Constants.ORG_ID, Long.class);
                // Set creation information and add beneficiary to the product.
                formula.setCreatedBy(userName);
                formula.setCreatedDate(new Date());
                formulaService.addFormulas(formula, Util.getCollectionNameWithOrgName(formula, String.valueOf(orgNameWithId)));
                // Return a successful response if the beneficiary addition is successful.
                return ResponseEntity.ok(Constants.FORMULA_ADDED_SUCCESSFULLY);
            }
            // Return an unauthorized response if the token is not valid or if user information cannot be extracted.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Constants.UNAUTHORIZED);
        } catch (Exception e) {
            // Handle any exceptions that might occur during the beneficiary addition process.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } finally {
            long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
            long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
            // Log the total execution time in milliseconds
            logger.info("addProduct API execution time (milliseconds): " + totalTimeMillis);
        }
    }

    /**
     * Retrieve all products for a specific organization.
     *
     * @return ResponseEntity containing a list of all products.
     */
    @SecurityRequirements({
            @SecurityRequirement(name = "Authorization")
    })
    @GetMapping("/getAllFormulas")
    public ResponseEntity<List<Formula>> getAllFormulas(
            @RequestHeader("Authorization") String token
    ) {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        List<Formula> formulas = new ArrayList<>();
        try {
            Claims claims = Util.validateTokenAndGetClaims(token);
            if (claims != null) {
                Long orgNameWithId = claims.get(Constants.ORG_ID, Long.class);
                // Call the service method to get all products
                formulas = formulaService.getFormulas(String.valueOf(orgNameWithId));
                long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
                long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
                // Log or use the 'totalTimeMillis' value as needed
                logger.info("API execution time (milliseconds): " + totalTimeMillis);
                return ResponseEntity.ok(formulas);
            }
            // Handle unauthorized or invalid token scenarios
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(formulas);
        } catch (Exception e) {
            // Handle exceptions and return an appropriate error response
            long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
            long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
            // Log or use the 'totalTimeMillis' value as needed
            logger.error("Error during API execution: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(formulas);
        }
    }

    /**
     * Update an existing product.
     *
     * @param formula The updated product information.
     * @param token   The JWT authorization token.
     * @return ResponseEntity indicating the success or failure of the operation.
     */
    @PutMapping("/updateFormulas")
    @SecurityRequirements({
            @SecurityRequirement(name = "Authorization") // This should match the security scheme defined in your OpenAPI definition
    })
    public ResponseEntity<String> updateFormulas(@RequestBody Formula formula,
                                                @RequestHeader("Authorization") String token) {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        try {
            Claims claims = Util.validateTokenAndGetClaims(token);
            if (claims != null) {
                String userName = claims.get(Constants.USER_NAME, String.class);
                Long orgNameWithId = claims.get(Constants.ORG_ID, Long.class);
                // Update product information and save changes
                Formula formula1 = formulaService.updateFormulas(formula, userName, String.valueOf(orgNameWithId));
                if(formula1.isDeleted()){
                    return ResponseEntity.ok(Constants.FORMULA_DELETED_SUCCESSFULLY);
                }
                long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
                long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
                // Log or use the 'totalTimeMillis' value as needed
                logger.info("API execution time (milliseconds): " + totalTimeMillis);
                return ResponseEntity.ok(Constants.FORMULA_UPDATED_SUCCESSFULLY);
            } else {
                // Handle unauthorized scenario if claims are not present
                throw new JwtException(Constants.INVALID_JWT_CLAIMS);
            }
        } catch (JwtException e) {
            logger.error("JWT Exception: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Constants.UNAUTHORIZED_COLON + e.getMessage());
        } catch (FormulaNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Constants.FORMULA_NOT_FOUND_COLON + e.getMessage());
        } catch (CollectionNotFoundException e) {
            logger.error("CollectionNotFoundException: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Constants.COLLECTION_NOT_FOUND + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected Exception: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}

