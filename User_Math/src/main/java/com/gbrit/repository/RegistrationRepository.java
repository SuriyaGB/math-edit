package com.gbrit.repository;

import com.gbrit.entity.Registration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends MongoRepository<Registration, Long> {
    Optional<Registration> findByEmail(String email);
    Optional<Registration> findByName(String name);
    List<Registration> findByIsDeleted(boolean isDeleted);
    @Query("{ $and: [ { 'isDeleted' : { $ne : true } }, { '_id': { $gt: 0 } } ] }")
    List<Registration> getAllRegisterUsers();
    @Query("{ $and: [ { 'isDeleted' : { $ne : true } }, { 'enabled': { $ne : true } }, { '_id': { $gt: 0 } } ] }")
    List<Registration> getAllRegisterUsersAndNonSignUpUsers();
    @Query("{ $and: [ { 'isDeleted' : { $ne : true } }, { 'enabled': { $ne : false } }, { '_id': { $gt: 0 } } ] }")
    List<Registration> getAllRegisterAndSignUp();
}
