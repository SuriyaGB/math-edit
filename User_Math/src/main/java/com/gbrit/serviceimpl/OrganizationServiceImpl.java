package com.gbrit.serviceimpl;

import com.gbrit.entity.Organization;
import com.gbrit.repository.OrganizationRepository;
import com.gbrit.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    @Autowired
    OrganizationRepository organizationRepository;

    /**
     * Save an organization in the repository.
     *
     * @param organization The organization entity to be saved.
     * @return The saved organization entity.
     */
    @Override
    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }

    /**
     * Find an organization by its ID.
     *
     * @param id The ID of the organization to find.
     * @return The found organization entity, or null if not found.
     */
    @Override
    public Organization findById(long id) {
        // Try to find an organization by its ID in the repository
        // If found, return the organization; otherwise, return null
        return organizationRepository.findById(id).orElse(null);
    }

    /**
     * Fetch all Organizations.
     *
     * @return      The list of Organizations.
     */
    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }
}
