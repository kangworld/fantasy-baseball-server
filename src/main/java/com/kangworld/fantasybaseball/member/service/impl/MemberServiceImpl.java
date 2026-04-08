package com.kangworld.fantasybaseball.member.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.exception.MemberEmailAlreadyExistsException;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberRole;
import com.kangworld.fantasybaseball.member.model.MemberStatus;
import com.kangworld.fantasybaseball.member.service.MemberService;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupRequest;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MemberSignupResponse signup(MemberSignupRequest request) {
        // 이메일 중복은 회원가입 전에 먼저 막는다.
        if (memberDao.findByEmail(request.getEmail()).isPresent()) {
            throw new MemberEmailAlreadyExistsException();
        }

        LocalDateTime now = LocalDateTime.now();

        Member member = Member.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(MemberRole.USER)
                .status(MemberStatus.OK)
                .emailVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        long memberId = memberDao.insert(member);

        return MemberSignupResponse.builder()
                .memberId(memberId)
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .build();
    }

}
