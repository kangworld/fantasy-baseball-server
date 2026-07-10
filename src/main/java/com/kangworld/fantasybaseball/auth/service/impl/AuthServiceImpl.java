package com.kangworld.fantasybaseball.auth.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.auth.exception.LoginFailedException;
import com.kangworld.fantasybaseball.auth.exception.RefreshTokenExpiredException;
import com.kangworld.fantasybaseball.auth.service.AuthService;
import com.kangworld.fantasybaseball.auth.service.dto.LoginMember;
import com.kangworld.fantasybaseball.auth.service.dto.LoginRequest;
import com.kangworld.fantasybaseball.auth.service.dto.LoginResponse;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshAccessTokenResponse;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenParts;
import com.kangworld.fantasybaseball.auth.service.dto.RefreshTokenValue;
import com.kangworld.fantasybaseball.auth.token.JwtAccessTokenFactory;
import com.kangworld.fantasybaseball.auth.token.RefreshTokenFactory;
import com.kangworld.fantasybaseball.auth.token.RefreshTokenStore;
import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtAccessTokenFactory jwtAccessTokenFactory;
    private final RefreshTokenFactory refreshTokenFactory;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public LoginResponse login(LoginRequest request) {
        Member member = memberDao.findByEmail(request.getEmail())
                .filter(savedMember -> savedMember.getStatus() == MemberStatus.OK)
                .orElseThrow(LoginFailedException::new);

        // 이메일 존재 여부와 비밀번호 불일치는 같은 실패로 묶는다.
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new LoginFailedException();
        }

        String accessToken = jwtAccessTokenFactory.create(member.getMemberId(), member.getRole().name());
        RefreshTokenValue refreshTokenValue = refreshTokenFactory.create(member.getMemberId());

        refreshTokenStore.save(refreshTokenValue);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue.getRawToken())
                .member(LoginMember.builder()
                        .memberId(member.getMemberId())
                        .email(member.getEmail())
                        .displayName(member.getDisplayName())
                        .build())
                .build();
    }

    @Override
    public RefreshAccessTokenResponse refreshAccessToken(String refreshToken) {
        RefreshTokenParts refreshTokenParts = refreshTokenFactory.parse(refreshToken);

        if (refreshTokenParts == null) {
            throw new RefreshTokenExpiredException();
        }

        Long memberId = refreshTokenParts.getUserId();
        Member member = memberDao.findByMemberId(memberId)
                .filter(savedMember -> savedMember.getStatus() == MemberStatus.OK)
                .orElseThrow(RefreshTokenExpiredException::new);

        String savedSecretHash = refreshTokenStore.findSecretHash(memberId, refreshTokenParts.getTokenId())
                .orElseThrow(RefreshTokenExpiredException::new);
        String requestSecretHash = refreshTokenFactory.hash(refreshTokenParts.getSecret());

        if (!savedSecretHash.equals(requestSecretHash)) {
            throw new RefreshTokenExpiredException();
        }

        RefreshTokenValue nextRefreshToken = refreshTokenFactory.create(memberId);
        refreshTokenStore.save(nextRefreshToken);
        // 새 토큰을 먼저 저장해 재발급 중간 실패로 기존 세션까지 잃지 않게 한 뒤 이전 토큰을 제거한다.
        refreshTokenStore.delete(memberId, refreshTokenParts.getTokenId());

        return RefreshAccessTokenResponse.builder()
                .accessToken(jwtAccessTokenFactory.create(memberId, member.getRole().name()))
                .refreshToken(nextRefreshToken.getRawToken())
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        // refresh token은 memberId.tokenId.secret 구조라서 회원과 tokenId를 분리해 삭제 키를 만든다.
        RefreshTokenParts refreshTokenParts = refreshTokenFactory.parse(refreshToken);

        if (refreshTokenParts == null) {
            return;
        }

        String savedSecretHash = refreshTokenStore.findSecretHash(
                        refreshTokenParts.getUserId(),
                        refreshTokenParts.getTokenId()
                )
                .orElse(null);
        String requestSecretHash = refreshTokenFactory.hash(refreshTokenParts.getSecret());

        if (!requestSecretHash.equals(savedSecretHash)) {
            return;
        }

        refreshTokenStore.delete(refreshTokenParts.getUserId(), refreshTokenParts.getTokenId());
    }

}
