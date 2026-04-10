package com.kangworld.fantasybaseball.auth.token;

import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;

public interface RefreshTokenStore {

    void save(RefreshTokenValue refreshTokenValue);

    void delete(Long userId, String tokenId);

}
