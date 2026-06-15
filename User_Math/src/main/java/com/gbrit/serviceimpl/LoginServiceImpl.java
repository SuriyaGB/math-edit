package com.gbrit.serviceimpl;

import com.gbrit.dto.LoginRequestDTO;
import com.gbrit.entity.RefreshToken;
import com.gbrit.entity.User;
import com.gbrit.exception.APIResponse;
import com.gbrit.service.LoginService;
import com.gbrit.service.RegistrationService;
import com.gbrit.util.AesUtils;
import com.gbrit.util.JwtUtils;
import com.gbrit.util.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RefreshTokenServiceImpl refreshTokenServiceImpl;

    @Autowired
    RegistrationService registrationService;

    /**
     * Perform user login and generate JWT token and refresh token.
     *
     * @param loginRequestDTO The DTO containing login credentials.
     * @return An APIResponse containing access token and refresh token.
     */
    @Override
    public ResponseEntity<APIResponse> login(LoginRequestDTO loginRequestDTO) {
        APIResponse apiResponse = new APIResponse();
        String userEmail = loginRequestDTO.getEmail();
        User user = registrationService.getUserByUserEmail(userEmail);
        if (user != null) {
            if (userEmail.equals(user.getEmail()) && user.getPassword().equals(loginRequestDTO.getPassword())) {
                RefreshToken refreshToken = refreshTokenServiceImpl.createRefreshToken(userEmail);
                String jwtToken = jwtUtils.generateJwt(user);
                apiResponse.setAccessToken(AesUtils.encryptAes(jwtToken, MessageConstants.SECRET_AES_KEY, MessageConstants.IV));
                apiResponse.setToken(refreshToken.getToken());
                return ResponseEntity.ok(apiResponse);
            } else {
                apiResponse.setError(MessageConstants.INVALID_CREDENTIALS);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
            }
        } else {
            apiResponse.setError(MessageConstants.USER_NOT_FOUND);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }
}