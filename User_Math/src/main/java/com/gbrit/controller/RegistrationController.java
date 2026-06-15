package com.gbrit.controller;

import com.gbrit.dto.SignUpRequestDTO;
import com.gbrit.dto.ValidationResponse;
import com.gbrit.entity.*;
import com.gbrit.eventlistener.RegistrationCompleteEvent;
import com.gbrit.dto.RegistrationRequest;
import com.gbrit.exception.APIResponse;
import com.gbrit.exception.MessagingException;
import com.gbrit.exception.UserNotFoundException;
import com.gbrit.service.OrganizationService;
import com.gbrit.service.RegistrationService;
import com.gbrit.util.AesUtils;
import com.gbrit.util.MessageConstants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/register")
public class RegistrationController {

    @Autowired
    RegistrationService registrationService;

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private final ApplicationEventPublisher publisher;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    OrganizationService organizationService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    RestTemplate restTemplate;

    @Value("${mathEditorServiceUrl}")
    String MATH_EDITOR_SERVICE_URL;

    @PostMapping("createMonitorUser")
    public void createMonitorUser() {
        User user = new User();
        user.setId(registrationService.generateSequence(MessageConstants.USER));
        user.setEmail(MessageConstants.MONITOR_USER_EMAIL);
        user.setForcePassword(MessageConstants.TRUE);
        user.setMonitoredUser(MessageConstants.TRUE);
        user.setApiCallCount(MessageConstants.ONE);
        user.setPassword(AesUtils.encryptAes(MessageConstants.MONITOR_PASSWORD, MessageConstants.SECRET_AES_KEY, MessageConstants.IV));
        mongoTemplate.save(user);
    }

    /**
     * The `registerUser` method is used to register a new User. This method internally creates a new organization.
     *
     * @param registrationRequest The registration request containing user details.
     * @return A ResponseEntity containing the ID of the created organization.
     */
    @PostMapping("/save")
    public ResponseEntity<String> registerUser(@RequestBody RegistrationRequest registrationRequest, @RequestHeader("domain") String domain) {
        long startTime = System.currentTimeMillis(); // Record the start time
        logger.info("API Execution Start Time :" + startTime);
        // Check whether the given email already exists
        if (registrationService.verifyEmail(registrationRequest)) {
            // Creating a new Organization object to set the organization name and create a new organization in the database
            Organization organization = new Organization();
            organization.setOrganizationId(registrationService.generateSequence(Organization.SEQUENCE_NAME));
            organization.setOrganizationName(registrationRequest.organizationName());
            organization.setCreatedDate(new Date());
            Organization savedOrganization = organizationService.save(organization);
            // Return a success response immediately
            ResponseEntity<String> response = ResponseEntity.ok().body(MessageConstants.REGISTRATION_SUCCESS_EMAIL);
            // Execute the backend job asynchronously using ExecutorService
            executorService.execute(() -> {
                Registration registeredUser = registrationService.registerUser(registrationRequest, savedOrganization.getOrganizationId());
                registerUserAsync(registeredUser, applicationUrl(domain));
                long endTime = System.currentTimeMillis(); // Record the end time
                logger.info("Backend Job End Time :" + endTime); // Log the backend job completion time
                long totalTime = (endTime - startTime); // Calculate the total time in seconds
                // Log or use the 'totalTime' value as needed
                logger.info("Backend Job execution time (seconds): " + totalTime);
            });
            // Log the API execution end time
            logger.info("API Execution End Time :" + System.currentTimeMillis());
            return response; // Return the success response immediately
        }
        // Return a response if the email doesn't exist
        return ResponseEntity.ok().body(MessageConstants.EMAIL_DOES_NOT_EXIST);
    }

    public void registerUserAsync(Registration registeredUser, String applicationUrl) {
        // This method will be executed asynchronously
        publisher.publishEvent(new RegistrationCompleteEvent(registeredUser, applicationUrl));
    }

    /**
     * This method completes the user registration process when the user fills in the remaining required fields and a password.
     *
     * @param signUpRequestDTO The sign-up request containing user details.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/completeUserRegistration")
    public ResponseEntity<String> completeUserRegistration(@RequestBody SignUpRequestDTO signUpRequestDTO,
                                                           @RequestHeader("token") String token,
                                                           @RequestHeader("orgNameWithId") String orgId,
                                                           @RequestHeader("orgName") String orgName) {
        try {
            long startTime = System.currentTimeMillis();
            // Validate input parameters
            if (token.isBlank() && !orgId.isBlank() && !orgName.isBlank()) {
                // If the email is not found, complete user registration with a default registrationUserId (0)
                registrationService.completeUserRegistration(signUpRequestDTO, 0, Long.parseLong(orgId), orgName);
//                // Perform remaining backend jobs asynchronously using a separate thread
//                executorService.execute(() -> performRemainingBackendJobs(signUpRequestDTO, Long.parseLong(orgId), orgName));
            } else {
                List<User> userList = registrationService.getAllUsers();
                // Check for duplicate user ID
                boolean isDuplicateUserId = userList.stream()
                        .anyMatch(user -> signUpRequestDTO.getBeneficiaryId() != null &&
                                user.getBeneficiaryId() != null && user.getBeneficiaryId().equals(signUpRequestDTO.getBeneficiaryId()));
                // Check for duplicate email
                boolean isDuplicateEmail = userList.stream()
                        .anyMatch(user -> signUpRequestDTO.getEmail() != null && user.getEmail() != null &&
                                user.getEmail().equals(signUpRequestDTO.getEmail()));
                if (isDuplicateUserId) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(MessageConstants.USER_ID_ALREADY_EXISTS + signUpRequestDTO.getBeneficiaryId());
                } else if (isDuplicateEmail) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(MessageConstants.EMAIL_ALREADY_EXISTS + signUpRequestDTO.getEmail());
                }

                // Check if the token exists in the database
                VerificationToken verificationToken = registrationService.getTokenByToken(token);
                if (verificationToken == null) {
                    throw new RuntimeException(MessageConstants.TOKEN_NOT_FOUND);
                }
                logger.info("verificationToken: " + verificationToken.getToken());
                long orgNameWithId = verificationToken.getOrganizationId();

                if (orgNameWithId > 0) {
                    Query query = new Query(Criteria.where(MessageConstants.UNDERSCORE_ID).is(orgNameWithId));
                    Organization existingCompanySetUp = mongoTemplate.findOne(query, Organization.class, MessageConstants.ORGANIZATION);

                    // Check if existingCompanySetUp is not null
                    if (existingCompanySetUp != null) {
                        // Set the currency
                        existingCompanySetUp.setCurrency(signUpRequestDTO.getCurrency());
                        try {
                            // Save the updated organization
                            mongoTemplate.save(existingCompanySetUp, MessageConstants.ORGANIZATION);
                            logger.info("Currency updated successfully.");
                        } catch (Exception e) {
                            // Handle any exceptions, e.g., database operation failures
                            logger.error("Error updating currency: {}", e.getMessage());
                        }
                    } else {
                        // Handle the case where organization with given ID is not found
                        logger.error("Organization not found with ID: {}", orgNameWithId);
                    }
                } else {
                    // Handle the case where orgNameWithId is 0
                    logger.error("Invalid organization ID: {}", orgNameWithId);
                }
                Organization savedOrganization = organizationService.findById(orgNameWithId);
                if (savedOrganization == null) {
                    logger.error(MessageConstants.ORGANIZATION_NOT_FOUND);
                }
                Optional<Registration> registration = registrationService.findById(verificationToken.getRegistrationId());
                if (registration.isPresent()) {
                    signUpRequestDTO.setEmail(registration.get().getEmail());
                    signUpRequestDTO.setUserName(registration.get().getName());
                    signUpRequestDTO.setUploaded(MessageConstants.FALSE);
                    long registrationUserId = registration.get().getRegistrationId();
                    assert savedOrganization != null;
                    signUpRequestDTO.setForcePassword(true);
                    registrationService.completeUserRegistration(signUpRequestDTO, registrationUserId, savedOrganization.getOrganizationId(), savedOrganization.getOrganizationName());
                }
                // Perform remaining backend jobs asynchronously using a separate thread
                try {
                    assert savedOrganization != null;
                    performRemainingBackendJobs(signUpRequestDTO, savedOrganization.getOrganizationId(), savedOrganization.getOrganizationName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            logger.info("Total processing time (milliseconds): " + totalTime);
            return ResponseEntity.ok(MessageConstants.USER_CREATED_SUCCESSFULLY);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Perform the remaining backend jobs synchronously (not annotated with @Async)
     */
    public void performRemainingBackendJobs(SignUpRequestDTO signUpRequestDTO,
                                            long orgNameWithId, String organizationName) {
        try {
            // Prepare headers for the HTTP request to the Beneficiary microservice
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(MessageConstants.ORG_NAME_WITH_ID, String.valueOf(orgNameWithId));
            // Create an HTTP entity with the signUpRequestDTO as the body
            HttpEntity<SignUpRequestDTO> requestEntity = new HttpEntity<>(signUpRequestDTO, headers);
            // Continue with other logic, e.g., creating collections for the user
            ResponseEntity<String> collectionsResponse = restTemplate.exchange(
                    MATH_EDITOR_SERVICE_URL + MessageConstants.CREATE_COLLECTIONS_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            if (collectionsResponse.getStatusCode() != HttpStatus.OK) {
                // Handle the case where creating collections failed
                logger.error("Failed to create collections.");
            }
            if (signUpRequestDTO.getBeneficiary() != null) {
                // Prepare headers for the HTTP request to the Beneficiary microservice's addBeneficiary endpoint
                HttpHeaders beneficiaryRequestHeaders = new HttpHeaders();
                beneficiaryRequestHeaders.setContentType(MediaType.APPLICATION_JSON);
                beneficiaryRequestHeaders.set(MessageConstants.USER_NAME, signUpRequestDTO.getUserName());
                beneficiaryRequestHeaders.set(MessageConstants.ORG_NAME_WITH_ID, String.valueOf(orgNameWithId));
                beneficiaryRequestHeaders.set(MessageConstants.ORGANIZATION_NAME, organizationName);
                beneficiaryRequestHeaders.set(MessageConstants.AUTHORIZATION, null);// Include the "orgNameWithId" header
                // Create an HTTP entity with the signUpRequestDTO as the body and the beneficiary headers
                HttpEntity<SignUpRequestDTO> beneficiaryRequestEntity = new HttpEntity<>(signUpRequestDTO, beneficiaryRequestHeaders);
                // Make an HTTP POST request to the Beneficiary microservice's addBeneficiary endpoint
                ResponseEntity<String> beneficiaryResponse = restTemplate.postForEntity(
                        MATH_EDITOR_SERVICE_URL + MessageConstants.ADD_BENEFICIARY,
                        beneficiaryRequestEntity,
                        String.class);
                if (beneficiaryResponse.getStatusCode() != HttpStatus.OK) {
                    logger.error("Failed to add beneficiary in the Beneficiary microservice. Status code: " + beneficiaryResponse.getStatusCode());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * * Endpoint to initiate the forgot password process.
     * @param userEmail              The email of the user whose password is to be reset.
     * @return                       ResponseEntity indicating the result of the forgot password request.
     * @throws MessagingException    if there's an issue with sending an email.
     */
    @PostMapping("/forgotPassword")
    public ResponseEntity<String> forgotPassword(@RequestHeader("userEmail") String userEmail, @RequestHeader("url") String setPassword_URL) throws MessagingException, jakarta.mail.MessagingException {
        long startTime = System.currentTimeMillis(); // Record the start time in milliseconds
        ResponseEntity<String> response;
        try {
            response = registrationService.forgotPassword(userEmail, setPassword_URL);
        } catch (RuntimeException e) {
            // Handle the case where the email is not found
            logger.error("Error in forgotPassword API: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        long endTime = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        // Log or use the timing information as needed
        logger.info("forgotPassword API execution time (milliseconds): " + totalTimeMillis);
        return response;
    }


    /**
     * Endpoint to set a new password for a user.
     *
     * @param token            The token associated with the password reset request.
     * @param newPassword      The new password to be set.
     * @return                 ResponseEntity indicating the result of the password reset request.
     */
    @PostMapping("/setPassword")
    public ResponseEntity<String> setPassword(@RequestParam("token") String token, @RequestParam("password") String newPassword) {
        long startTime = System.currentTimeMillis(); // Record the start time in milliseconds
        ResponseEntity<String> response = registrationService.passwordSet(token, newPassword);
        long endTime = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        // Log or use the timing information as needed
        logger.info("setPassword API execution time (milliseconds): " + totalTimeMillis);
        return response;
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody SignUpRequestDTO signUpRequestDTO) {
        long startTime = System.currentTimeMillis(); // Record the start time in milliseconds
        String oldPassword = AesUtils.encryptAes(signUpRequestDTO.getOldPassword(), MessageConstants.SECRET_AES_KEY, MessageConstants.IV);
        String password = signUpRequestDTO.getPassword();
        try {
            registrationService.resetPassword(oldPassword, password);
            long endTime = System.currentTimeMillis(); // Record the end time in milliseconds
            long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
            // Log or use the timing information as needed
            logger.info("resetPassword API execution time (milliseconds): " + totalTimeMillis);
            return ResponseEntity.ok(MessageConstants.PASSWORD_RESET_SUCCESSFULLY);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return the error message
        }
    }

    @PostMapping("/passwordReset")
    public ResponseEntity<String> passwordReset(@RequestHeader("userId") String userId,@RequestHeader("oldPassword") String oldPassword,@RequestHeader("currentPassword") String currentPassword,
                                                @RequestHeader("orgId") long orgId) {
        long startTime = System.currentTimeMillis(); // Record the start time in milliseconds
        String oldPass = AesUtils.encryptAes(oldPassword, MessageConstants.SECRET_AES_KEY, MessageConstants.IV);
        try {
            registrationService.passwordReset(userId, oldPass, currentPassword, orgId);
            long endTime = System.currentTimeMillis(); // Record the end time in milliseconds
            long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
            // Log or use the timing information as needed
            logger.info("resetPassword API execution time (milliseconds): " + totalTimeMillis);
            return ResponseEntity.ok(MessageConstants.PASSWORD_RESET_SUCCESSFULLY);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return the error message
        }
    }

    /**
     * Endpoint to update user information.
     *
     * @param signUpRequestDTO     The request containing updated user information.
     * @return                     ResponseEntity containing the result of the update operation.
     */
    @PutMapping("/updateUser")
    public ResponseEntity<APIResponse> updateUser(@RequestBody SignUpRequestDTO signUpRequestDTO,
                                                  @RequestHeader("orgNameWithId") String orgId,
                                                  @RequestHeader("orgName") String orgName) {
        long startTime = System.currentTimeMillis(); // Record the start time in milliseconds
        APIResponse apiResponse = registrationService.updateUser(orgId, orgName, signUpRequestDTO);
        long endTime = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        // Log or use the timing information as needed
        logger.info("updateUser API execution time (milliseconds): " + totalTimeMillis);
        return ResponseEntity
                .status(apiResponse.getStatus())
                .body(apiResponse);
    }

    /**
     * Endpoint to edit user information.
     *
     * @param signUpRequestDTO     The request containing updated user information.
     * @return                     ResponseEntity containing the result of the update operation.
     */
    @PutMapping("/editUser")
    public ResponseEntity<String> editUser(@RequestBody SignUpRequestDTO signUpRequestDTO) {
        long startTime = System.currentTimeMillis(); // Record the start time in milliseconds
        ResponseEntity<String> responseEntity = registrationService.editUser(signUpRequestDTO);
        long endTime = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        // Log or use the timing information as needed
        logger.info("updateUser API execution time (milliseconds): " + totalTimeMillis);
        return responseEntity;
    }

    @GetMapping("/getAllUsersBasedOnType")
    public ResponseEntity<List<User>> getAllUsersBasedOnType(@RequestHeader("type") String type) {
        List<User> users;
        long startTime = System.currentTimeMillis(); // Record the start time
        switch (type) {
            case MessageConstants.REGISTERED_AND_SIGN_UP_USERS -> users = registrationService.getAllRegisteredUserAndSignUpUser();
            case MessageConstants.UPLOADED_USERS -> users = registrationService.getAllUploadedUsers();
            case MessageConstants.APPROVED -> users = registrationService.getAllApprovedUsers();
            case MessageConstants.NON_APPROVED -> users = registrationService.getAllNonApprovedUsers();
            default -> {
                return ResponseEntity.badRequest().build(); // Invalid type parameter
            }
        }
        long endTime = System.currentTimeMillis(); // Record the end time
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        logger.info("getAllUsers (" + type + ") execution time (milliseconds): " + totalTimeMillis);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/getAllRegistrationsBasedOnType")
    public ResponseEntity<List<Registration>> getAllRegistrationsBasedOnType(@RequestHeader("type") String type) {
        List<Registration> users;
        long startTime = System.currentTimeMillis(); // Record the start time
        switch (type) {
            case MessageConstants.GET_ALL_REGISTRATION -> users = registrationService.getAllRegisterUsers();
            case MessageConstants.NON_SIGNUP_REGISTRATION -> users = registrationService.getAllRegisterUsersAndNonSignUpUsers();
            case MessageConstants.SIGNUP_REGISTRATION -> users = registrationService.getAllRegisterAndSignUp();
            default -> {
                return ResponseEntity.badRequest().build(); // Invalid type parameter
            }
        }
        long endTime = System.currentTimeMillis(); // Record the end time
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        logger.info("getAllRegistrations (" + type + ") execution time (milliseconds): " + totalTimeMillis);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Endpoint to delete a user.
     *
     * @param id         The ID of the user to be deleted.
     * @return           ResponseEntity indicating the result of the delete operation.
     */
    @DeleteMapping("/deleted/{id}")
    public ResponseEntity<String> deleteEntity(@PathVariable long id) {
        long startTime = System.currentTimeMillis(); // Record the start time
        try {
            registrationService.deleteEntity(id);
            long endTime = System.currentTimeMillis(); // Record the end time
            long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
            logger.info("deleteEntity execution time (milliseconds): " + totalTimeMillis);
            return ResponseEntity.ok(MessageConstants.USER_DELETED_SUCCESSFULLY);
        } catch (UserNotFoundException e) {
            long endTime = System.currentTimeMillis(); // Record the end time
            long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
            logger.info("deleteEntity execution time (milliseconds): " + totalTimeMillis);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/deleteBeneficiary")
    public void updateBeneficiaries(@RequestHeader("email") String email, @RequestHeader("id") String id){
        registrationService.deleteBeneficiary(email, id);
    }

    /**
     * Endpoint to fetch all users and return them in a ResponseEntity.
     *
     * @return        ResponseEntity containing the list of all users.
     */
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<User>> getAllUsers() {
        long startTime = System.currentTimeMillis(); // Record the start time
        List<User> users = registrationService.getAllUsers();
        long endTime = System.currentTimeMillis(); // Record the end time
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        logger.info("getAllUsers execution time (milliseconds): " + totalTimeMillis);
        return new ResponseEntity<>(users, HttpStatus.CREATED);
    }

    @GetMapping("/getAllUserEmail")
    public ResponseEntity<List<String>> getAllUserEmail() {
        long startTime = System.currentTimeMillis(); // Record the start time
        List<String> users = registrationService.getAllUserEmail();
        long endTime = System.currentTimeMillis(); // Record the end time
        long totalTimeMillis = endTime - startTime; // Calculate the total time in milliseconds
        logger.info("getAllUsers execution time (milliseconds): " + totalTimeMillis);
        return ResponseEntity.ok(users);
    }

    /**
     * Endpoint to verify a token.
     *
     * @param token        The token to be verified.
     * @return             String indicating the verification result.
     */
    @GetMapping("/verifyToken")
    public ValidationResponse verifyToken(@RequestParam("token") String token) {
        try {
            return registrationService.verifyToken(token);
        } catch (Exception e) {
            // Handle exception and return appropriate error message
            return new ValidationResponse(MessageConstants.ERROR_DURING_TOKEN_VERIFICATION, null, null, null);
        }
    }

    @GetMapping("/verifyResetPasswordToken")
    public ResponseEntity<String> verifyResetPasswordToken(@RequestParam("token") String token) {
        try {
            String result = registrationService.verifyResetPasswordToken(token);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MessageConstants.ERROR_DURING_TOKEN_VERIFICATION);
        }
    }

    /**
     * Endpoint to retrieve a list of all Organizations.
     *
     * @return        List of all Organizations.
     */
    @GetMapping("/getAllOrganizations")
    public List<Organization> getAllOrganizations() {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        List<Organization> organizations = organizationService.getAllOrganizations();
        long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
        // Log or use the 'totalTimeMillis' value as needed
        logger.info("getAllOrganizations execution time (milliseconds): " + totalTimeMillis);
        return organizations;
    }

    /**
     * Endpoint to retrieve a list of all Registration.
     *
     * @return        List of all Registration.
     */
    @GetMapping("/getAllRegistrations")
    public List<Registration> getAllRegistrations() {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        List<Registration> registrations = registrationService.getAllRegistrations();
        long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
        long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
        // Log or use the 'totalTimeMillis' value as needed
        logger.info("getAllRegistrations execution time (milliseconds): " + totalTimeMillis);
        return registrations;
    }

    /**
     * Generate the application URL for a given organization.
     *
     * @param domain
     * @return                  The generated application URL.
     */
    public String applicationUrl(String domain) {
        return domain; // Modify this based on your application's logic
    }

    /**
     * Check if an email already exists.
     *
     * @param user       The user registration data containing the email to check.
     * @return           ResponseEntity indicating whether the email is matched or not.
     */
    @PostMapping("/checkEmail")
    public ResponseEntity<String> checkEmail(@RequestBody Registration user) {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        boolean nameMatched = registrationService.checkName(user.getName());
        long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
        // Calculate the total time in milliseconds
        long totalTimeMillis = endTimeMillis - startTimeMillis;
        // Log the start time, end time, and execution time in milliseconds
        logger.info("API start time (milliseconds): " + startTimeMillis);
        logger.info("API end time (milliseconds): " + endTimeMillis);
        logger.info("API execution time (milliseconds): " + totalTimeMillis);
        if (nameMatched) {
            return ResponseEntity.ok(MessageConstants.NAME_MATCHED);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MessageConstants.NAME_NOT_MATCHED);
        }
    }
}
