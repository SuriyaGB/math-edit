package com.gbrit.security;

import com.gbrit.entity.Organization;
import com.gbrit.entity.Registration;
import com.gbrit.repository.OrganizationRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class UserRegistrationDetails implements UserDetails {

    private String userName;
    private String password;
    private boolean isEnabled;
    private List<GrantedAuthority> authorities;

    @Autowired
    OrganizationRepository organizationRepository;

    /**
     * Constructor to create UserRegistrationDetails from a Registration entity.
     *
     * @param user       The Registration entity from which to create the UserRegistrationDetails.
     */
    public UserRegistrationDetails(Registration user) {
        // Set username, enabled status, and authorities based on the Registration entity
        this.userName = user.getEmail();
        this.isEnabled = user.isEnabled();
        long organizationId = user.getOrganizationId();
        Optional<Organization> organization = organizationRepository.findById(organizationId);
        // Extract organization names, split into a list, and map to SimpleGrantedAuthority
        this.authorities = Arrays.stream(organization.get().getOrganizationName()
                        .split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
