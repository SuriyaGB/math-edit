package com.gbrit.service;

import com.gbrit.entity.RefreshToken;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public interface RefreshTokenService {
    RefreshToken createRefreshToken(String userName);
    Optional<RefreshToken> findByToken(String token);
    RefreshToken verifyToken(RefreshToken token);
}
