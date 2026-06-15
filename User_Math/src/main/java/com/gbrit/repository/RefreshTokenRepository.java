package com.gbrit.repository;

import com.gbrit.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByToken(String token);
}

