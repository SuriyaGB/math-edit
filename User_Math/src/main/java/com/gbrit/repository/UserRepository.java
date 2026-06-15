package com.gbrit.repository;

import com.gbrit.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, Long> {
//    @Query("{ $and: [ { 'isDeleted' : { $ne : true } }, { 'isError': { $ne : true } }, { '_id': { $gt: 1 } } ] }")

    @Query("{ $and: [ { 'isDeleted' : { $ne : true } }, { 'isError': { $ne : true } } ] }")
    List<User> findAllActiveUsers();
    @Query("{ 'isDeleted' : { $ne : true }, 'isError': { $ne : true }, 'isMonitoredUser': { $ne : false } }")
    List<User> getMonitoredUsers();
    @Query("{ $and: [ { 'isDeleted': { $ne: true } }, { 'isError': { $ne: true } }, { '_id': { $gt: 1 } }, { 'isUploaded': { $ne: true } } ] }")
    List<User> getAllRegisteredUserAndSignUpUser();
    @Query("{ $and: [ { 'isDeleted': { $ne: true } }, { 'isError': { $ne: true } }, { '_id': { $gt: 1 } }, { 'isUploaded': { $ne: false } } ] }")
    List<User> getAllUploadedUsers();
    @Query("{ $and: [ { 'isDeleted': { $ne: true } }, { 'isError': { $ne: true } }, { '_id': { $gt: 1 } }, { 'isApproved': { $ne: false } } ] }")
    List<User> getAllApprovedUsers();
    @Query("{ $and: [ { 'isDeleted': { $ne: true } }, { 'isError': { $ne: true } }, { '_id': { $gt: 1 } }, { 'isApproved': { $ne: true } } ] }")
    List<User> getAllNonApprovedUsers();
    @Query("{ $and: [ { 'isDeleted' : { $ne : true } }, { 'isError': { $ne : true } }, { '_id': { $gt: 0 } } ] }")
    List<User> getUsers();
}

