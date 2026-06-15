package com.gbrit.eventlistener;

import com.gbrit.entity.Organization;
import com.gbrit.repository.OrganizationRepository;
import com.gbrit.service.RegistrationService;
import com.gbrit.util.MessageConstants;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RegistrationCompleteEventListener implements ApplicationListener<RegistrationCompleteEvent> {

    @Autowired
    TaskExecutor taskExecutor;

    private static final Logger logger = LoggerFactory.getLogger(RegistrationCompleteEventListener.class);

    private final RegistrationService registrationService;
    private final JavaMailSender mailSender;
    private com.gbrit.entity.Registration user;

    @Autowired
    OrganizationRepository organizationRepository;

    /**
     * Handle the registration complete event.
     *
     * @param event               The RegistrationCompleteEvent.
     */
    @Override
    public void onApplicationEvent(RegistrationCompleteEvent event) {
        user = event.getUser();
        // Generate a verification token
        String verificationToken = UUID.randomUUID().toString();
        registrationService.saveUserVerificationToken(user, verificationToken);
        // Create the verification URL
        //MessageConstants.VERIFY_EMAIL
        String url = event.getApplicationUrl() + MessageConstants.HASH + verificationToken;
        // Send the verification email asynchronously
        taskExecutor.execute(() -> {
            try {
                sendVerificationEmail(url);
                // Log the registration event after successful email sending
                logger.info(MessageConstants.VERIFY_REGISTRATION, url);
            } catch (Exception e) {
                // Handle exceptions
                logger.error("Error sending verification email for user " + user.getName(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Send a verification email to the user.
     *
     * @param url                               The verification URL to include in the email.
     * @throws UnsupportedEncodingException     if there is an encoding issue.
     * @throws jakarta.mail.MessagingException   if there is a problem with the email content.
     */
    public void sendVerificationEmail(String url) throws UnsupportedEncodingException, jakarta.mail.MessagingException {
        long organizationId = user.getOrganizationId();
        String userName = user.getName();
        Optional<Organization> organization = organizationRepository.findById(organizationId);
        // Prepare email details
        String subject = MessageConstants.EMAIL_VERIFICATION;
        String senderName = MessageConstants.USER_REGISTRATION_PORTAL_SERVICE;
        String mailContent = String.format(
                MessageConstants.REGISTRATION_VERIFICATION_TEMPLATE,
                userName,
                url
        );
        // Create and send the email
        MimeMessage message = mailSender.createMimeMessage();
        var messageHelper = new MimeMessageHelper(message);
        messageHelper.setFrom(MessageConstants.SENDER_EMAIL, senderName);
        messageHelper.setTo(user.getEmail());
        messageHelper.setSubject(subject);
        messageHelper.setText(mailContent, true);
        mailSender.send(message);
    }
}
