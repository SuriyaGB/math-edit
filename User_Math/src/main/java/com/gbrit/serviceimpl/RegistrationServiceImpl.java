package com.gbrit.serviceimpl;

import com.gbrit.dto.SignUpRequestDTO;
import com.gbrit.dto.ValidationResponse;
import com.gbrit.entity.*;
import com.gbrit.exception.*;
import com.gbrit.dto.RegistrationRequest;
import com.gbrit.repository.RegistrationRepository;
import com.gbrit.repository.UserRepository;
import com.gbrit.repository.VerificationTokenRepository;
import com.gbrit.service.RegistrationService;
import com.gbrit.util.AesUtils;
import com.gbrit.util.MessageConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import java.util.Objects;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    RegistrationRepository registrationRepository;

    @Autowired
    VerificationTokenRepository tokenRepository;

    @Autowired
    JavaMailSender javaMailSender;

    private final MongoOperations mongoOperations;

    @Autowired
    TaskExecutor taskExecutor;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    public RegistrationServiceImpl(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    /**
     * Generate a new sequence value for the specified sequence name.
     *
     * @param seqName The name of the sequence.
     * @return The new sequence value.
     */
    public long generateSequence(String seqName) {
        // Find and modify the sequence counter in the database
        DatabaseSequence counter = mongoOperations.findAndModify(
                query(where(MessageConstants.ID_FIELD).is(seqName)),
                new Update().inc(MessageConstants.SEQ, 1),
                options().returnNew(true).upsert(true),
                DatabaseSequence.class);
        // If counter exists, return the updated sequence value; otherwise, return 1
        return !Objects.isNull(counter) ? counter.getSeq() : 1;
    }

    /**
     * Verify the uniqueness of the provided email during registration.
     *
     * @param request       The registration request containing the email.
     * @return              Return `true` if the email is unique; otherwise, throw an exception.
     */
    @Override
    public boolean verifyEmail(RegistrationRequest request) {
        // Check if a user with the provided email already exists
        Optional<Registration> registeredUser = registrationRepository.findByEmail(request.email());
        // If user with the email exists, throw a UserAlreadyExistsException
        if (registeredUser.isPresent()) {
            throw new UserAlreadyExistsException(
                    MessageConstants.USER_WITH_EMAIL + request.email() + MessageConstants.ALREADY_EXISTS);
        }
        // Email is unique
        return true;
    }

    /**
     * Register a new user based on the provided registration request and organization.
     *
     * @param request         The registration request.
     * @return                The registered user entity.
     */
    @Override
    public Registration registerUser(RegistrationRequest request, long organizationId) {
        // Create a new Registration object
        Registration registerUser = new Registration();
        // Verify the uniqueness of the email
        if (verifyEmail(request)) {
            // Generate a unique ID using the generateSequence method
            registerUser.setRegistrationId(generateSequence(Registration.SEQUENCE_NAME));
            // Populate user details from the registration request and organization
            registerUser.setName(request.name().trim());
            registerUser.setEmail(request.email());
            registerUser.setOrganizationId(organizationId);
            registerUser.setPhone(request.phone());
            registerUser.setCountry(request.country());
            // Save the registered user to the database
            registrationRepository.save(registerUser);
        }
        // Return the registered user entity
        return registerUser;
    }

    /**
     * This method updates the registration table and inserts a new user record into the user table.
     *
     * @param signUpRequestDTO   The DTO containing user registration information.
     * @param registrationUserId The ID of the registration entry associated with this user.
     */
    @Override
    public void completeUserRegistration(SignUpRequestDTO signUpRequestDTO, long registrationUserId, long organizationId, String organizationName) {
        try {
            List<User> userList = getAllUsers();
            List<User> usersInOrganization = userList.stream()
                    .filter(user -> user.getOrganizationId() == organizationId)
                    .toList();

            boolean isDuplicateEmail = userList.stream()
                    .anyMatch(user -> signUpRequestDTO.getEmail() != null && user.getEmail() != null &&
                            user.getEmail().equals(signUpRequestDTO.getEmail()));

            boolean isDuplicateUserId = usersInOrganization.stream()
                    .anyMatch(user -> signUpRequestDTO.getBeneficiaryId() != null &&
                            user.getBeneficiaryId() != null && user.getBeneficiaryId().equals(signUpRequestDTO.getBeneficiaryId()));

            List<User> duplicateUsers = usersInOrganization.stream()
                    .filter(user -> signUpRequestDTO.getRole() != 1 && user.getBeneficiaryId().equals(signUpRequestDTO.getBeneficiaryId())
                            && user.getEmail().equals(signUpRequestDTO.getEmail()) && signUpRequestDTO.getUserName() != null)
                    .toList();

            List<User> duplicateAdminUsers = usersInOrganization.stream()
                    .filter(user -> signUpRequestDTO.getRole() == 1 && user.getEmail().equals(signUpRequestDTO.getEmail()) && signUpRequestDTO.getUserName() != null
                            && signUpRequestDTO.getBeneficiaryId() != null)
                    .toList();

            List<User> duplicateSalesRepUsers = usersInOrganization.stream()
                    .filter(user -> signUpRequestDTO.getUserName() != null &&
                            signUpRequestDTO.getBeneficiaryId() != null &&
                            ((user.getBeneficiaryId().equals(signUpRequestDTO.getBeneficiaryId()) &&
                                    !user.getEmail().equals(signUpRequestDTO.getEmail())) ||
                                    (!user.getBeneficiaryId().equals(signUpRequestDTO.getBeneficiaryId()) &&
                                            user.getEmail().equals(signUpRequestDTO.getEmail()))))
                    .toList();

            if (!isDuplicateUserId && !isDuplicateEmail && signUpRequestDTO.getEmail() != null && signUpRequestDTO.getUserName() != null) {
                User userEntity = createUserEntity(signUpRequestDTO, organizationId, organizationName);
                userRepository.save(userEntity);
            } else {
                    for (User user : duplicateAdminUsers) {
                        if (!user.isUploaded() && user.getBeneficiaryId() != null && user.getEmail() != null && user.getUserName() != null) {
                            user.setError(true);
                            user.setDeleted(true);
                            user.setDescription(MessageConstants.NEW_RECORD_ARRIVED);
                            userRepository.save(user);
                            if(user.getRole() == signUpRequestDTO.getRole()) {
                                User userEntity = createEntity(signUpRequestDTO, organizationId, organizationName);
                                userRepository.save(userEntity);
                            }
                        } else {
                            if (signUpRequestDTO.isUploaded() && signUpRequestDTO.getRole() == 1) {
                                signUpRequestDTO.setError(true);
                                signUpRequestDTO.setDescription(MessageConstants.USER_IS_NOT_ADMIN + signUpRequestDTO.getBeneficiaryId() + MessageConstants.AND + signUpRequestDTO.getEmail() + MessageConstants.AND + signUpRequestDTO.getUserName());
                                User userEntity = createEntity(signUpRequestDTO, organizationId, organizationName);
                                userRepository.save(userEntity);
                            }
                        }
                        if (signUpRequestDTO.getBeneficiaryId() == null || signUpRequestDTO.getEmail() == null || signUpRequestDTO.getUserName() == null) {
                            // Handling null values in the request
                            signUpRequestDTO.setError(true);
                            signUpRequestDTO.setDescription(MessageConstants.BENEFICIARY_USER_ID_EMAIL_USER_NAME_NULL + signUpRequestDTO.getBeneficiaryId() + MessageConstants.AND + signUpRequestDTO.getEmail() + MessageConstants.AND + signUpRequestDTO.getUserName());
                            User userEntity = createEntity(signUpRequestDTO, organizationId, organizationName);
                            userRepository.save(userEntity);
                        }
                    }

                // Mark existing records with duplicate email or user ID as error
                for (User user : duplicateUsers) {
                    if (user.getBeneficiaryId() != null && user.getEmail() != null && user.getUserName() != null) {
                        user.setError(true);
                        user.setDeleted(true);
                        user.setDescription(MessageConstants.NEW_RECORD_ARRIVED);
                        userRepository.save(user);
                    }
                    if (signUpRequestDTO.getRole() != 1) {
                        User userEntity = createUserEntity(signUpRequestDTO, organizationId, organizationName);
                        userRepository.save(userEntity);
                    }
                }

                // Mark existing records with duplicate email or user ID as error
                for (User user : duplicateSalesRepUsers) {
                    if (user.isUploaded() && user.getBeneficiaryId() != null && user.getEmail() != null && user.getUserName() != null) {
                        // Handling null values in the request
                        signUpRequestDTO.setError(true);
                        signUpRequestDTO.setDescription(MessageConstants.BENEFICIARY_USER_ID_EMAIL_ANYONE_ONLY_EXISTS + signUpRequestDTO.getBeneficiaryId() + MessageConstants.AND_EMAIL + signUpRequestDTO.getEmail());
                        User userEntity = createEntity(signUpRequestDTO, organizationId, organizationName);
                        userRepository.save(userEntity);
                    }
                }
            }

            // Verify and enable the registration user ID
            if (registrationUserId != 0) {
                Registration registration = registrationRepository.findById(registrationUserId)
                        .orElseThrow(() -> new RuntimeException(MessageConstants.USER_NOT_FOUND));
                if (registration != null) {
                    registration.setEnabled(true);
                    registrationRepository.save(registration);
                }
            }
        } catch (DuplicateEmailException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(MessageConstants.ERROR, e);
        }
    }

    private User createEntity(SignUpRequestDTO signUpRequestDTO, long organizationId, String organizationName) {
        User userEntity = new User();
        String userName = AesUtils.cleanUsername(signUpRequestDTO.getUserName());
        userEntity.setUserName(userName);
        userEntity.setPassword(signUpRequestDTO.getPassword());
        userEntity.setBeneficiary(signUpRequestDTO.getBeneficiary());
        userEntity.setRole(signUpRequestDTO.getRole());
        userEntity.setRoleName(signUpRequestDTO.getRoleName());
        userEntity.setBeneficiaryId(signUpRequestDTO.getBeneficiaryId());
        boolean forcePassword = signUpRequestDTO.isForcePassword();
        userEntity.setForcePassword(forcePassword);
        userEntity.setEmail(signUpRequestDTO.getEmail());
        userEntity.setJob(signUpRequestDTO.getJob());
        userEntity.setTerritory(signUpRequestDTO.getTerritory());
        userEntity.setReportingTo(signUpRequestDTO.getReportingTo());
        userEntity.setReportingToName(signUpRequestDTO.getReportingToName());
        userEntity.setStartDate(signUpRequestDTO.getStartDate());
        userEntity.setEndDate(signUpRequestDTO.getEndDate());
        userEntity.setEligible(signUpRequestDTO.isEligible());
        userEntity.setEmployeeType(signUpRequestDTO.getEmployeeType());
        userEntity.setNotes(signUpRequestDTO.getNotes());
        userEntity.setUploaded(signUpRequestDTO.isUploaded());
        userEntity.setCustomFields(signUpRequestDTO.getCustomFields());
        userEntity.setOrganizationId(organizationId);
        userEntity.setOrganizationName(organizationName);
        userEntity.setCreatedBy(signUpRequestDTO.getCreatedBy());
        userEntity.setCreatedDate(signUpRequestDTO.getCreatedDate());
        userEntity.setError(signUpRequestDTO.isError());
        userEntity.setDescription(signUpRequestDTO.getDescription());
        userEntity.setId(generateSequence(MessageConstants.USER));
        return userEntity;
    }

    private User createUserEntity(SignUpRequestDTO signUpRequestDTO, long organizationId, String organizationName) {
        User userEntity = new User();
        String userName = AesUtils.cleanUsername(signUpRequestDTO.getUserName());
        userEntity.setUserName(userName);
        userEntity.setPassword(signUpRequestDTO.getPassword());
        userEntity.setBeneficiary(signUpRequestDTO.getBeneficiary());
        userEntity.setRole(signUpRequestDTO.getRole() == 0 ? 1 : signUpRequestDTO.getRole());
        userEntity.setRoleName(signUpRequestDTO.getRoleName() == null ? MessageConstants.ADMIN : signUpRequestDTO.getRoleName());
        userEntity.setBeneficiaryId(signUpRequestDTO.getBeneficiaryId() == null ? organizationName.substring(0, 1) + generateSequence(organizationName + organizationId) : signUpRequestDTO.getBeneficiaryId());
        boolean forcePassword = signUpRequestDTO.isForcePassword();
        userEntity.setForcePassword(forcePassword);
        userEntity.setUploaded(signUpRequestDTO.isUploaded());
        userEntity.setEmail(signUpRequestDTO.getEmail());
        userEntity.setJob(signUpRequestDTO.getJob());
        userEntity.setTerritory(signUpRequestDTO.getTerritory());
        userEntity.setReportingTo(signUpRequestDTO.getReportingTo() == null ? organizationName.substring(0, 1) + generateSequence(organizationName) : signUpRequestDTO.getReportingTo());
        userEntity.setReportingToName(signUpRequestDTO.getReportingToName() == null ? userName : signUpRequestDTO.getReportingToName());
        userEntity.setStartDate(signUpRequestDTO.getStartDate());
        userEntity.setEndDate(signUpRequestDTO.getEndDate());
        userEntity.setEligible(signUpRequestDTO.isEligible());
        userEntity.setEmployeeType(signUpRequestDTO.getEmployeeType());
        userEntity.setNotes(signUpRequestDTO.getNotes());
        userEntity.setCustomFields(signUpRequestDTO.getCustomFields());
        userEntity.setOrganizationId(organizationId);
        userEntity.setOrganizationName(organizationName);
        userEntity.setCreatedBy(signUpRequestDTO.getCreatedBy());
        userEntity.setCreatedDate(signUpRequestDTO.getCreatedDate() != null ? signUpRequestDTO.getCreatedDate() : new Date());
        userEntity.setId(generateSequence(MessageConstants.USER));
        return userEntity;
    }

    /**
        * Generate a password reset token for the user and send a reset email.
        * @param users  The user for whom the password reset token is generated.
     */
    public boolean generatePasswordResetToken(List<User> users, String userEmail, String setPassword_URL) {
        String token = UUID.randomUUID().toString();
        boolean emailFound = false;
        for (User userList : users) {
            if (userEmail.equals(userList.getEmail())) {
                long userId = userList.getId();
                String userName = userList.getUserName();
                Query query = new Query(Criteria.where(MessageConstants.UNDERSCORE_ID).is(userId));
                Update update = new Update().set(MessageConstants.RESET_TOKEN, token);
                mongoTemplate.updateFirst(query, update, User.class, MessageConstants.USER);
                // Construct the reset link and email content
                String resetLink = setPassword_URL + MessageConstants.HASH + token;
                String emailBody = String.format(MessageConstants.PASSWORD_RESET_TEMPLATE, userName, resetLink);
                // Send the email to the user's email address
                taskExecutor.execute(() -> {
                    try {
                        sendEmail(userList.getEmail(), emailBody);
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                });
                emailFound = true;
                // Break out of the loop once the email is found and processed
                break;
            }
        }
        return emailFound;
    }

    /**
     * Send a password reset email to the user.
     *
     * @param userEmail               The email address of the user.
     * @return                        ResponseEntity with a message indicating success or failure.
     */
    public ResponseEntity<String> forgotPassword(String userEmail, String setPassword_URL) {
        List<User> users = getAllUsers();
        boolean emailFound = generatePasswordResetToken(users, userEmail, setPassword_URL);
        if (emailFound) {
            return ResponseEntity.ok().body(MessageConstants.PASSWORD_RESET_EMAIL_SENT);
        } else {
            throw new RuntimeException(MessageConstants.EMAIL_NOT_FOUND);
        }
    }

    /**
     * Reset the user's password using the provided token and new password.
     *
     * @param token                    The password reset token.
     * @param newPassword              The new password to set.
     * @return                         ResponseEntity indicating the result of the password reset.
     */
    public ResponseEntity<String> passwordSet(String token, String newPassword) {
        List<User> userList = getAllUsers();
        boolean validToken = false;
        for (User user : userList) {
            // Check if the reset token is not null before comparing
            if (token.equals(user.getResetToken())) {
                validToken = true;
                long userId = user.getId();
                setPassword(newPassword, userId);
                break; // No need to continue looping once a valid token is found
            }
        }
        if (validToken) {
            return ResponseEntity.ok(MessageConstants.PASSWORD_RESET_SUCCESS);
        } else {
            return ResponseEntity.badRequest().body(MessageConstants.INVALID_TOKEN);
        }
    }

    /**
     * Set the new password for the user using the provided token.
     *
     * @param newPassword      The new password to set.
     */
    public void setPassword(String newPassword, long userId) {
        Query query = new Query(Criteria.where(MessageConstants.UNDERSCORE_ID).is(userId));
        Update update = new Update().set(MessageConstants.PASSWORD, newPassword).set(MessageConstants.RESET_TOKEN, null).set(MessageConstants.FORCE_PASSWORD, true);
        mongoTemplate.updateFirst(query, update, User.class, MessageConstants.USER);
    }

    /**
     * Email the registered user.
     *
     * @param to                      The recipient's email address.
     * @param content                 The content of the email.
     */
    public void sendEmail(String to, String content) throws jakarta.mail.MessagingException {
        // Create a MIME message and set its properties
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(MessageConstants.SENDER_EMAIL);
        helper.setTo(to);
        helper.setSubject(MessageConstants.PASSWORD_RESET);
        helper.setText(content, true);
        // Send the email using the JavaMailSender
        javaMailSender.send(message);
    }

    /**
     * Fetch all Registrations.
     *
     * @return      The list of Registrations.
     */
    @Override
    public List<Registration> getAllRegistrations() {
        return registrationRepository.findByIsDeleted(true);
    }

    /**
     * Update a user's information based on the provided ID and data.
     *
     * @param signUpRequestDTO   The updated user data.
     * @return                   APIResponse indicating the success of the update.
     */
    @Override
    public APIResponse updateUser(String organizationId, String organizationName, SignUpRequestDTO signUpRequestDTO) {
        APIResponse apiResponse = new APIResponse();

        Query query = new Query(Criteria.where(MessageConstants.BENEFICIARY_ID).is(signUpRequestDTO.getBeneficiaryId())
                .and(MessageConstants.ORG_ID).is(Long.parseLong(organizationId))
                .and(MessageConstants.IS_DELETED).ne(true)
                .and(MessageConstants.IS_ERROR).ne(true));
        User user = mongoTemplate.findOne(query, User.class, MessageConstants.USER);
        
        if(user != null) {
            String userName = AesUtils.cleanUsername(signUpRequestDTO.getUserName());
            user.setUserName(userName);
            user.setPassword(signUpRequestDTO.getPassword());
            user.setBeneficiary(signUpRequestDTO.getBeneficiary());
            user.setRole(signUpRequestDTO.getRole() == 0 ? 1 : signUpRequestDTO.getRole());
            user.setRoleName(signUpRequestDTO.getRoleName() == null ? MessageConstants.ADMIN : signUpRequestDTO.getRoleName());
            user.setBeneficiaryId(signUpRequestDTO.getBeneficiaryId() == null ? organizationName.substring(0, 1) + generateSequence(organizationName + organizationId) : signUpRequestDTO.getBeneficiaryId());
            boolean forcePassword = signUpRequestDTO.isForcePassword();
            user.setForcePassword(forcePassword);
            user.setEmail(signUpRequestDTO.getEmail());
            user.setJob(signUpRequestDTO.getJob());
            user.setTerritory(signUpRequestDTO.getTerritory());
            user.setReportingTo(signUpRequestDTO.getReportingTo() == null ? organizationName.substring(0, 1) + generateSequence(organizationName) : signUpRequestDTO.getReportingTo());
            user.setReportingToName(signUpRequestDTO.getReportingToName() == null ? userName : signUpRequestDTO.getReportingToName());
            user.setStartDate(signUpRequestDTO.getStartDate());
            user.setEndDate(signUpRequestDTO.getEndDate());
            user.setEligible(signUpRequestDTO.getUserName() != null && !signUpRequestDTO.getUserName().isEmpty());
            user.setEmployeeType(signUpRequestDTO.getEmployeeType());
            user.setNotes(signUpRequestDTO.getNotes());
            user.setCustomFields(signUpRequestDTO.getCustomFields());
            user.setOrganizationId(Long.parseLong(organizationId));
            user.setOrganizationName(organizationName);
            user.setCreatedBy(signUpRequestDTO.getCreatedBy());
            user.setCreatedDate(signUpRequestDTO.getCreatedDate() != null ? signUpRequestDTO.getCreatedDate() : new Date());
            user.setUpdatedBy(signUpRequestDTO.getUpdatedBy());
            user.setUpdatedDate(signUpRequestDTO.getUpdatedDate() != null ? signUpRequestDTO.getUpdatedDate() : new Date());
            user.setError(false);
            userRepository.save(user);
            apiResponse.setAccessToken(MessageConstants.USER_UPDATED);
            return apiResponse;
        } else {
            apiResponse.setErrorDetails(MessageConstants.USER_NOT_FOUND);
            return apiResponse;
        }
    }

    /**
     * Delete a user entity based on the provided ID.
     *
     * @param id     The ID of the user to delete.
     */
    @Override
    public void deleteEntity(long id) {
        // Retrieve the user by ID or throw an exception if not found
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(MessageConstants.USER_NOT_FOUND));
        // Set the 'deleted' flag to true to mark the user as not deleted
        if (user != null) {
            user.setDeleted(true);
            userRepository.save(user);
        } else {
            throw new UserNotFoundException(MessageConstants.ENTITY_NOT_FOUND + id);
        }
    }

    @Override
    public void deleteBeneficiary(String email, String beneficiaryId) {
        // Retrieve the user by email
        List<User> userList = userRepository.findAllActiveUsers();
        boolean userFound = false;
        for (User user : userList) {
            // Check if the user exists and if the beneficiaryId matches
            if (user.getEmail().equals(email) && user.getBeneficiaryId().equals(beneficiaryId)) {
                // Mark the user as deleted
                user.setDeleted(true);
                user.setDeletedDate(new Date());
                userRepository.save(user);
                userFound = true;
                break; // Exit the loop once user is found and updated
            }
        }
        if (!userFound) {
            throw new UserNotFoundException(MessageConstants.ENTITY_NOT_FOUND + email);
        }
    }

    /**
     * Save the user verification token.
     *
     * @param theUser       The user for whom the token is saved.
     * @param token         The verification token.
     */
    @Override
    public void saveUserVerificationToken(Registration theUser, String token) {
        // Create a new verification token and save it to the repository
        var verificationToken = new VerificationToken(token, theUser);
        verificationToken.setId(generateSequence(VerificationToken.SEQUENCE_NAME));
        verificationToken.setRegistrationId(theUser.getRegistrationId());
        verificationToken.setOrganizationId(theUser.getOrganizationId());
        tokenRepository.save(verificationToken);
    }

    @Override
    public ValidationResponse verifyToken(String token) {
        long startTimeMillis = System.currentTimeMillis();
        try {
            Optional<VerificationToken> optionalToken = Optional.ofNullable(tokenRepository.findByToken(token));
            if (optionalToken.isPresent()) {
                VerificationToken theToken = optionalToken.get();
                long registrationId = theToken.getRegistrationId();
                Optional<Registration> optionalRegistration = registrationRepository.findById(registrationId);
                if (optionalRegistration.isPresent()) {
                    Registration registration = optionalRegistration.get();
                    if (!registration.isEnabled()) {
                        return validateToken(token);
                    }else {
                        return new ValidationResponse(MessageConstants.SIGN_UP_ALREADY_SUCCESS, null, null, null);
                    }
                } else {
                    return new ValidationResponse(MessageConstants.INVALID_VERIFICATION_TOKEN, null, null, null);
                }
            } else {
                return new ValidationResponse(MessageConstants.TOKEN_NOT_FOUND, null, null, null);
            }
        } catch (Exception e) {
            logger.error("Error during token verification: {}", e.getMessage());
            return new ValidationResponse(MessageConstants.INVALID_TOKEN, null, null, null); // Provide an appropriate error message
        } finally {
            long endTimeMillis = System.currentTimeMillis();
            long totalTimeMillis = endTimeMillis - startTimeMillis;
            logger.info("verifyToken execution time (milliseconds): {}", totalTimeMillis);
        }
    }

    @Override
    public List<String> getAllUserEmail() {
        List<User> userList = getAllUsers();
        List<String> userEmail = new ArrayList<>();
        for (User user : userList) {
            userEmail.add(user.getEmail());
        }
        return userEmail;
    }

    @Override
    public String verifyResetPasswordToken(String token) {
        List<User> userList = getAllUsers();
        for (User user : userList) {
            if (token.equals(user.getResetToken())) {
                return MessageConstants.VALID;
            }
        }
        return MessageConstants.INVALID_VERIFICATION_TOKEN;
    }

    /**
     * Validate a verification token.
     *
     * @param theToken      The token to validate.
     * @return              A string indicating the validation result.
     */
    @Override
    public ValidationResponse validateToken(String theToken) {
        // Find the verification token by its value
        VerificationToken token = tokenRepository.findByToken(theToken);
        if (token == null) {
            return new ValidationResponse(MessageConstants.INVALID_VERIFICATION_TOKEN, null, null , null);
        }
        long registrationId = token.getRegistrationId();
        // Retrieve the associated user
        Registration user = registrationRepository.findById(registrationId).orElse(null);
        if (user == null) {
            return new ValidationResponse(MessageConstants.USER_NOT_FOUND, null, null, null);
        }
        Calendar calendar = Calendar.getInstance();
        // Check if the token has expired
        if ((token.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0) {
            // Delete the expired token and mark the user as disabled
            tokenRepository.delete(token);
            return new ValidationResponse(MessageConstants.TOKEN_ALREADY_EXPIRED, null, null, null);
        }
        String email = user.getEmail();
        String organizationId = String.valueOf(user.getOrganizationId());
        String userName = user.getName();
        logger.info("email :" + email);
        user.setEnabled(false);
        registrationRepository.save(user);
        return new ValidationResponse(MessageConstants.VALID, email, organizationId, userName);
    }

    /**
     * Fetch all users.
     *
     * @return      The list of all users.
     */
    @Override
    public List<User> getAllUsers() {
     return userRepository.findAllActiveUsers();
    }

    @Override
    public List<User> getUsers() {
        return userRepository.getUsers();
    }

    @Override
    public List<User> getMonitoredUsers() {
        return userRepository.getMonitoredUsers();
    }

    /**
     * Check if the provided email exists in the database.
     *
     * @param userName      The userName to check.
     * @return               Return `true` if the email exists; otherwise, throw an exception.
     */
    @Override
    public boolean checkName(String userName) {
        // Find a user by email or return a default empty Registration object
        Registration user = registrationRepository.findByName(userName).orElse(new Registration());
        if (user.getName() == null) {
            // If the email doesn't exist, throw a UserNotFoundException
            throw new UserNotFoundException(MessageConstants.USER_NOT_FOUND_NAME + userName);
        }
        String storedEmail = user.getName();
        // Compare provided email with stored email and return result
        return userName.equals(storedEmail);
    }

    @Override
    public void resetPassword(String oldPassword, String newPassword) {
        // Query for the user with the old password
        Query query = new Query(Criteria.where(MessageConstants.PASSWORD).is(oldPassword)
                .and(MessageConstants.IS_DELETED).ne(true)
                .and(MessageConstants.IS_ERROR).ne(true));
        User user = mongoTemplate.findOne(query, User.class, MessageConstants.USER);
        if (user != null) {
            // User found, update the password and save
            user.setPassword(newPassword);
            user.setForcePassword(true);
            userRepository.save(user);
        } else {
            // Handle the case where the user is not found or the old password is incorrect
            throw new RuntimeException(MessageConstants.USER_NOT_FOUND_MESSAGE);
        }
    }

    @Override
    public void passwordReset(String userId, String oldPass, String currentPassword, long orgId) {
        // Query for the user with the userId
        Query query = new Query(Criteria.where(MessageConstants.BENEFICIARY_ID).is(userId)
                .and(MessageConstants.ORG_ID).is(orgId)
                .and(MessageConstants.IS_DELETED).ne(true)
                .and(MessageConstants.IS_ERROR).ne(true));
        User user = mongoTemplate.findOne(query, User.class, MessageConstants.USER);
        if (user != null) {
            // Check if the old password matches
            if (user.getPassword().equals(oldPass)) {
                // Update the password and save
                user.setPassword(currentPassword);
                user.setForcePassword(true);
                userRepository.save(user);
            } else {
                // Handle the case where the old password is incorrect
                throw new RuntimeException(MessageConstants.INVALID_OLD_PASSWORD_MESSAGE);
            }
        } else {
            // Handle the case where the user is not found
            throw new RuntimeException(MessageConstants.USER_NOT_FOUND_MESSAGE);
        }
    }

    @Override
    public ResponseEntity<String> editUser(SignUpRequestDTO signUpRequestDTO) {
        try {
            List<User> userList = userRepository.findAllActiveUsers();
            // Filter the userList based on beneficiaryId and email
            Optional<User> userOptional = userList.stream()
                    .filter(user -> user.getBeneficiaryId().equals(signUpRequestDTO.getBeneficiaryId()) &&
                            user.getEmail().equals(signUpRequestDTO.getEmail()))
                    .findFirst(); // Use findFirst() to get the first matching user if any
            User user = userOptional.orElseThrow(() -> new UserNotFoundException(MessageConstants.USER_NOT_FOUND));
            // Update the user with the new approval status
            user.setApproved(signUpRequestDTO.isApproved());
            // Save the updated user
            userRepository.save(user);
            String responseMessage;
            if (user.isApproved()) {
                responseMessage = user.getUserName() + MessageConstants.APPROVED_SUCCESSFULLY;
            } else {
                responseMessage = user.getUserName() + MessageConstants.UNAPPROVED_SUCCESSFULLY;
            }
            return ResponseEntity.ok(responseMessage);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // Return appropriate response
        }
    }

    @Override
    public List<User> getAllRegisteredUserAndSignUpUser() {
        return userRepository.getAllRegisteredUserAndSignUpUser();
    }

    @Override
    public List<User> getAllUploadedUsers() {
        return userRepository.getAllUploadedUsers();
    }

    @Override
    public List<User> getAllApprovedUsers() {
        return userRepository.getAllApprovedUsers();
    }

    @Override
    public List<User> getAllNonApprovedUsers() {
        return userRepository.getAllNonApprovedUsers();
    }

    @Override
    public List<Registration> getAllRegisterUsers() {
        return registrationRepository.getAllRegisterUsers();
    }

    @Override
    public List<Registration> getAllRegisterUsersAndNonSignUpUsers() {
        return registrationRepository.getAllRegisterUsersAndNonSignUpUsers();
    }

    @Override
    public List<Registration> getAllRegisterAndSignUp() {
        return registrationRepository.getAllRegisterAndSignUp();
    }

    @Override
    public VerificationToken getTokenByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Override
    public User getUserByUserEmail(String userEmail) {
        List<User> userList = getUsers();
        for (User user : userList) {
            if (user.getEmail().equals(userEmail)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public Optional<Registration> findById(long registrationId) {
        return registrationRepository.findById(registrationId);
    }
}