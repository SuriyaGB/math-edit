package com.gbrit.serviceImpl;

import com.gbrit.entity.*;
import com.gbrit.exception.*;
import com.gbrit.service.FormulaService;
import com.gbrit.utils.Constants;
import com.gbrit.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.util.*;
import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Slf4j
public class FormulaServiceImpl implements FormulaService {

    private static final Logger logger = LoggerFactory.getLogger(FormulaServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    MongoOperations mongoOperations;


    /**
     * Add a new product to the collection.
     *
     * @param formula        The product to add.
     * @param collectionName The name of the collection to add the product to.
     */
    @Override
    public void addFormulas(Formula formula, String collectionName) {
        // Check if the collection exists
        if (mongoTemplate.collectionExists(collectionName)) {
            // Generate an ID for the product
            formula.setId(generateSequence(collectionName));
            // Save the product to the collection
            mongoTemplate.save(formula, collectionName);
        } else {
            throw new CollectionNotFoundException(Constants.COLLECTION_DOES_NOT_EXIST + collectionName);
        }
    }

    /**
     * Update a product in the collection.
     *
     * @param formula       The updated product.
     * @param userName      The username of the user performing the update.
     * @param orgNameWithId The organization-specific name with ID.
     */
    @Override
    public Formula updateFormulas(Formula formula, String userName, String orgNameWithId) {
        String collectionName = Util.getCollectionNameWithOrgName(formula, orgNameWithId);
        // Check if the collection exists
        if (mongoTemplate.collectionExists(collectionName)) {
            // Check if the product with the given ID exists in the collection
            Query query = new Query(Criteria.where(Constants.ID).is(formula.getId())
                    .and(Constants.IS_ERROR).ne(true)
                    .and(Constants.IS_DELETED).ne(true));
            Formula existingFormula = mongoTemplate.findOne(query, Formula.class, collectionName);
            if (existingFormula == null) {
                throw new FormulaNotFoundException(Constants.FORMULA_WITH_ID_EXIST + formula.getId() + Constants.DOES_NOT_EXIST);
            }

            // Preserve original creation information
            Date createdDate = existingFormula.getCreatedDate();
            String createdBy = existingFormula.getCreatedBy();
            // Set updated fields
            formula.setCreatedBy(createdBy);
            formula.setCreatedDate(createdDate);
            formula.setUpdatedBy(userName);
            formula.setUpdatedDate(new Date());
            // Save the updated product entry
            return mongoTemplate.save(formula, collectionName);
        } else {
            throw new CollectionNotFoundException(Constants.COLLECTION_DOES_NOT_EXIST + collectionName);
        }
    }


    /**
     * Generate a new sequence value using MongoDB atomic operations.
     *
     * @param seqName The name of the sequence.
     * @return The new sequence value.
     */
    @Override
    public long generateSequence(String seqName) {
        // Find and increment the sequence using MongoDB atomic operations
        DatabaseSequence counter = mongoOperations.findAndModify(query(where(Constants.ID_FIELD).is(seqName)),
                new Update().inc(Constants.SEQ, 1), options().returnNew(true).upsert(true),
                DatabaseSequence.class);
        // Return the new sequence value
        return !Objects.isNull(counter) ? counter.getSeq() : 1;
    }

    /**
     * Get a list of all active products for a specific organization.
     *
     * @param orgNameWithId        The organization-specific name with ID.
     * @return                     A list of active products.
     */
    @Override
    public List<Formula> getFormulas(String orgNameWithId) {
        Formula formula = new Formula();
        // Check if the collection exists
        if (mongoTemplate.collectionExists(Util.getCollectionNameWithOrgName(formula, orgNameWithId))) {
            // Create a query to retrieve non-deleted products
            Query query = new Query(Criteria.where(Constants.ID).gt(Constants.ZERO).and(Constants.IS_DELETED).ne(true).and(Constants.IS_ERROR).ne(true));
            // Return the list of active products
            return mongoTemplate.find(query, Formula.class, Util.getCollectionNameWithOrgName(formula, orgNameWithId));
        } else {
            throw new CollectionNotFoundException(Constants.COLLECTION_DOES_NOT_EXIST + Util.getCollectionNameWithOrgName(formula, orgNameWithId));
        }
    }
}

