package com.gbrit.controller;

import com.gbrit.dto.LoginRequestDTO;
import com.gbrit.entity.RefreshToken;
import com.gbrit.exception.APIResponse;
import com.gbrit.service.LoginService;
import com.gbrit.service.RefreshTokenService;
import com.gbrit.util.AesUtils;
import com.gbrit.util.JwtUtils;
import com.gbrit.util.MessageConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class LoginController {

    @Autowired
    LoginService loginService;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RefreshTokenService refreshTokenService;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * Endpoint for user login.
     *
     * @param loginRequestDTO The login request data.
     * @return ResponseEntity containing the API response.
     */
    @PostMapping("/login")
    public ResponseEntity<APIResponse> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        APIResponse apiResponse = loginService.login(loginRequestDTO).getBody();
        assert apiResponse != null;
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    /**
     * Endpoint to verify a JWT token.
     *
     * @param jwtToken The JWT token provided in the authorization header.
     * @return ResponseEntity containing the API response indicating token validation.
     */
    @GetMapping("/verifyToken")
    @SecurityRequirements({
            @SecurityRequirement(name = "Authorization") // This should match the security scheme defined in your OpenAPI definition
    })
    public ResponseEntity<String> verifyToken(@RequestHeader("Authorization") String jwtToken) {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        String auth = AesUtils.decryptAes(jwtToken, MessageConstants.SECRET_AES_KEY, MessageConstants.IV);
        logger.info("Decrypted Token: " + auth);
        try {
            // Check if the token starts with "Bearer"
            if (auth != null && auth.startsWith(MessageConstants.AUTHORIZATION_BEARER)) {
                String token = auth.substring(7); // Remove the "Bearer " prefix
                // Verify the JWT token using jwtUtils.verify
                jwtUtils.verify(token);
            } else {
                // Verify the JWT token as-is (without "Bearer" prefix)
                jwtUtils.verify(auth);
            }
            long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
            long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
            // Log or use the total time as needed
            logger.info("/verifyToken execution time (milliseconds): " + totalTimeMillis);
            return ResponseEntity.ok(MessageConstants.JWT_TOKEN_VALIDATED);
        } catch (Exception e) {
            // Handle token verification failure
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageConstants.JWT_TOKEN_VERIFICATION_FAILED);
        }
    }

    /**
     * Endpoint to refresh an access token using a refresh token.
     *
     * @param token
     * @return                       The API response with a new access token and the same refresh token.
     */
    @PostMapping("/refreshToken")
    public APIResponse refreshToken(@RequestHeader("token") String token) {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        // Find the refresh token using refreshTokenService.findByToken
        APIResponse response = refreshTokenService.findByToken(token)
                // Verify the expiration of the refresh token using refreshTokenService.verifyExpiration
                .map(refreshTokenService::verifyToken)
                // Get the user associated with the refresh token
                .map(RefreshToken::getUser)
                // Generate a new access token using jwtUtils.generateJwt
                .map(user -> {
                    String accessToken = jwtUtils.generateJwt(user);
                    // Build an API response with the new access token and the same refresh token
                    return APIResponse.builder()
                            .status(HttpStatus.OK.value())
                            .accessToken(accessToken)
                            .token(token)
                            .build();
                })
                // If any of the steps fail, throw an exception indicating that the refresh token is expired
                .orElseThrow(() -> new RuntimeException(MessageConstants.REFRESH_TOKEN_EXPIRED));
        long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
        // Log or use the total time as needed
        logger.info("/refreshToken execution time (milliseconds): " + totalTimeMillis);
        return response;
    }
}
