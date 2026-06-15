package com.gbrit.service;

import com.gbrit.entity.Organization;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface OrganizationService {
    Organization save(Organization organization);
    Organization findById(long orgId);
    List<Organization> getAllOrganizations();
}
