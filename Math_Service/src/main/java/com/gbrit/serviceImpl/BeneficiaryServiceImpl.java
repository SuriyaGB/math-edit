package com.gbrit.serviceImpl;

import com.gbrit.entity.*;
import com.gbrit.exception.*;
import com.gbrit.service.BeneficiaryService;
import com.gbrit.utils.Constants;
import com.gbrit.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.util.Objects;
import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private static final Logger logger = LoggerFactory.getLogger(BeneficiaryServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    MongoOperations mongoOperations;


    /**
     * Add a new beneficiary to the specified collection.
     * Generates a unique ID using a sequence and saves the beneficiary entry.
     *
     * @param beneficiary    The beneficiary to be added.
     * @param collectionName The name of the collection to add the beneficiary to.
     */
    public void addBeneficiary(Beneficiary beneficiary, String collectionName, String orgNameWithId, String orgName) {
        // Check if the collection exists
        if (mongoTemplate.collectionExists(collectionName)) {


            if (beneficiary.getEmail() != null && beneficiary.getUserId() == null) {
                // If email is empty, set default values and save the beneficiary
                beneficiary.setRole(1);
                beneficiary.setRoleName(Constants.ADMIN);
                beneficiary.setReportingTo(orgName.substring(0, 1) + generateSequence(orgNameWithId + orgName));
                beneficiary.setForcePassword(true);
                if (beneficiary.getUserName() != null && !beneficiary.getUserName().isEmpty()) {
                    beneficiary.setEligible(true);
                } else {
                    beneficiary.setEligible(false);
                }
                beneficiary.setError(false);
                beneficiary.setUserId(orgName.substring(0, 1) + generateSequence(orgNameWithId));
                beneficiary.setOrganizationId(Long.parseLong(orgNameWithId));
                beneficiary.setOrganizationName(orgName);
                String userName = Util.cleanUsername(beneficiary.getUserName());
                beneficiary.setUserName(userName);
                beneficiary.setReportingToName(userName);
                beneficiary.setId(generateSequence(collectionName));
                mongoTemplate.save(beneficiary, collectionName);
            }
        }
    }

    @Override
    public long generateSequence(String seqName) {
        // Find and increment the sequence using MongoDB atomic operations
        DatabaseSequence counter = mongoOperations.findAndModify(
                query(where(Constants.ID_FIELD).is(seqName)),
                new Update().inc(Constants.SEQ, 1),
                options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );
        // Return the new sequence value
        return !Objects.isNull(counter) ? counter.getSeq() : 1;
    }
}



