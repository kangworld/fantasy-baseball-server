package com.kangworld.fantasybaseball.auth.token;

import java.util.Optional;

import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;

public interface RefreshTokenStore {

    void save(RefreshTokenValue refreshTokenValue);

    Optional<String> findSecretHash(Long userId, String tokenId);

    void delete(Long userId, String tokenId);

    void deleteAll(Long userId);

}
