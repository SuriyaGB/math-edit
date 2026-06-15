package com.gbrit.repository;

import com.gbrit.entity.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrganizationRepository extends MongoRepository<Organization, Long> {
    Organization findByOrganizationName(String organizationName);
}
