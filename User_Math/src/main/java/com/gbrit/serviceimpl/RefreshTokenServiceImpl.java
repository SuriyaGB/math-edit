package com.gbrit.serviceimpl;

import com.gbrit.entity.RefreshToken;
import com.gbrit.entity.User;
import com.gbrit.repository.RefreshTokenRepository;
import com.gbrit.service.RefreshTokenService;
import com.gbrit.util.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    RegistrationServiceImpl registrationService;

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * Create a new refresh token for the given user.
     *
     * @param userEmail        The username of the user.
     * @return                The created RefreshToken instance.
     */
    @Override
    public RefreshToken createRefreshToken(String userEmail) {
        String collectionName = MessageConstants.USER ;
        Query query = new Query(Criteria.where(MessageConstants.EMAIL)
                .regex(MessageConstants.UP_ARROW + userEmail + MessageConstants.DOLLAR, MessageConstants.I));
        query.collation(Collation.of(MessageConstants.EN).strength(Collation.ComparisonLevel.primary()));
        User user = mongoTemplate.findOne(query, User.class, collectionName);
        if (user == null) {
            // Handle the case where the user with the specified userName doesn't exist
            return null;
        }
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .build();
        refreshToken.setId(registrationService.generateSequence(RefreshToken.SEQUENCE_NAME));
        return refreshTokenRepository.save(refreshToken);
    }


    /**
     * Find a refresh token by its token string.
     *
     * @param token         The token string to search for.
     * @return              An optional containing the found RefreshToken, or an empty optional if not found.
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify if a refresh token is valid.
     *
     * @param token                   The RefreshToken instance to verify.
     * @return                        The same RefreshToken instance.
     * @throws RuntimeException       if the token is invalid.
     */
    @Override
    public RefreshToken verifyToken(RefreshToken token) {
        if (token == null) {
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }
        return token;
    }
}