package com.gbrit.service;

import com.gbrit.dto.SignUpRequestDTO;
import com.gbrit.dto.ValidationResponse;
import com.gbrit.entity.Organization;
import com.gbrit.entity.Registration;
import com.gbrit.entity.User;
import com.gbrit.dto.RegistrationRequest;
import com.gbrit.entity.VerificationToken;
import com.gbrit.exception.APIResponse;
import com.gbrit.exception.DuplicateUserNameException;
import com.gbrit.exception.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public interface RegistrationService {
    List<Registration> getAllRegistrations();
    APIResponse updateUser(String orgId, String orgName, SignUpRequestDTO signUpRequestDTO);
    void completeUserRegistration(SignUpRequestDTO signUpRequestDTO, long registrationUserId, long organizationId, String organizationName) throws DuplicateUserNameException;
    void deleteEntity(long id);
    void deleteBeneficiary(String email, String id);
    boolean verifyEmail(RegistrationRequest request);
    Registration registerUser(RegistrationRequest request, long organizationId);
    void saveUserVerificationToken(Registration user, String verificationToken);
    ValidationResponse validateToken(String theToken);
    ResponseEntity<String> forgotPassword(String userEmail, String setPassword_URL) throws MessagingException, jakarta.mail.MessagingException;
    ResponseEntity<String> passwordSet(String token, String newPassword);
    long generateSequence(String sequenceName);
    List<User> getAllUsers();

    List<User> getUsers();

    List<User> getMonitoredUsers();
    boolean checkName(String userName);
    void resetPassword(String oldPassword, String newPassword);
    VerificationToken getTokenByToken(String token);
    Optional<Registration> findById(long registrationId);
    ValidationResponse verifyToken(String token);
    String verifyResetPasswordToken(String token);
    User getUserByUserEmail(String userEmail);
    List<String> getAllUserEmail();
    void passwordReset(String userId, String oldPass, String currentPassword, long orgId);
    ResponseEntity<String> editUser(SignUpRequestDTO signUpRequestDTO);
    List<User> getAllRegisteredUserAndSignUpUser();
    List<User> getAllUploadedUsers();
    List<User> getAllApprovedUsers();
    List<User> getAllNonApprovedUsers();
    List<Registration> getAllRegisterUsers();
    List<Registration> getAllRegisterUsersAndNonSignUpUsers();
    List<Registration> getAllRegisterAndSignUp();
}
