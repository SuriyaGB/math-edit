package com.gbrit.controller;

import com.gbrit.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/baseControl")
public class BaseController {

    // Inject the MongoTemplate bean for database operations
    @Autowired
    MongoTemplate mongoTemplate;

    // Inject the collection names from configuration
    @Value("${collection}")
    String[] collectionNames;

    // Logger instance for this class
    private final Logger logger = LoggerFactory.getLogger(BaseController.class);

    /**
     * Endpoint for creating collections and saving entities.
     *
     * @param orgNameWithId     The organization name with ID from request header
     * @return                  ResponseEntity containing the status and response message
     */
    @PostMapping("/createCollections")
    public ResponseEntity<String> createCollections(@RequestHeader String orgNameWithId) {
        long startTimeMillis = System.currentTimeMillis(); // Record the start time in milliseconds
        try {
            // Iterate through the configured collection names
            for (String collectionName : collectionNames) {
                // Construct the fully qualified class name
                String className = Constants.ENTITY_PACKAGE + collectionName;
                // Load the class using reflection
                Class<?> c = Class.forName(className);
                logger.info(Constants.OBJECT_NAME_PLACEHOLDER, c.getName());
                // Create a new instance of the class using its default constructor
                Object entityInstance = c.getDeclaredConstructor().newInstance();
                // Save the new object to the database with a modified collection name
                mongoTemplate.save(entityInstance, collectionName + Constants.UNDERSCORE + orgNameWithId);
                logger.info(Constants.COLLECTION_CREATED, collectionName);
            }
            // Return a success response with an OK status
            long endTimeMillis = System.currentTimeMillis(); // Record the end time in milliseconds
            long totalTimeMillis = endTimeMillis - startTimeMillis; // Calculate the total time in milliseconds
            // Log the total execution time
            logger.info("API execution time (milliseconds): " + totalTimeMillis);
            return ResponseEntity.ok(Constants.COLLECTIONS_CREATED_SUCCESSFULLY);
        } catch (ReflectiveOperationException ex) {
            // Handle exceptions by logging and returning an error response
            logger.error(Constants.ERROR_CREATING_COLLECTIONS, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Constants.ERROR_CREATING_COLLECTIONS);
        }
    }
}