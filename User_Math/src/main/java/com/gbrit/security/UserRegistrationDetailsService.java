package com.gbrit.security;

import com.gbrit.repository.RegistrationRepository;
import com.gbrit.util.MessageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegistrationDetailsService implements UserDetailsService {

    @Autowired
    RegistrationRepository registrationRepository;

    /**
     * Load user details by email for authentication.
     *
     * @param email                          The email of the user to load.
     * @return                               User details for authentication.
     * @throws UsernameNotFoundException     if user is not found.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find user by email in the registration repository
        return registrationRepository.findByEmail(email)
                .map(UserRegistrationDetails::new) // Create UserRegistrationDetails from found user
                .orElseThrow(() -> new UsernameNotFoundException(MessageConstants.USER_NOT_FOUND)); // Throw exception if user not found
    }
}
