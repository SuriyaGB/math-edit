package com.gbrit.eventlistener;

import com.gbrit.entity.Registration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class RegistrationCompleteEvent extends ApplicationEvent {

    private Registration user;
    private String applicationUrl;
    /**
     * Constructor for the RegistrationCompleteEvent.
     *
     * @param user                 The registered user.
     * @param applicationUrl       The URL associated with the registration process.
     */
    public RegistrationCompleteEvent(Registration user, String applicationUrl) {
        super(user); // Call the constructor of the superclass ApplicationEvent
        this.user = user;
        this.applicationUrl = applicationUrl;
    }
}
