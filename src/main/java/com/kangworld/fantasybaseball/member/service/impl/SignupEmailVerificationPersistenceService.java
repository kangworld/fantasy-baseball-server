package com.kangworld.fantasybaseball.member.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.dao.SignupEmailVerificationDao;
import com.kangworld.fantasybaseball.member.exception.MemberEmailAlreadyExistsException;
import com.kangworld.fantasybaseball.member.exception.SignupEmailVerificationRequiredException;
import com.kangworld.fantasybaseball.member.model.SignupEmailVerification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupEmailVerificationPersistenceService {

    private final MemberDao memberDao;
    private final SignupEmailVerificationDao signupEmailVerificationDao;

    @Transactional
    public void saveCode(String email, String codeHash, LocalDateTime codeExpiresAt, LocalDateTime now) {
        if (memberDao.findByEmail(email).isPresent())
            throw new MemberEmailAlreadyExistsException();

        // 같은 이메일의 진행 중 인증은 하나만 둔다. 재전송은 이전 인증번호와 인증 완료 상태를 무효화한다.
        Optional<SignupEmailVerification> verification = signupEmailVerificationDao.findActiveByEmail(email, now);
        if (verification.isEmpty()) {
            signupEmailVerificationDao.insert(email, codeHash, codeExpiresAt, now);
            return;
        }

        int updatedCount = signupEmailVerificationDao.updateCode(
                verification.get().getSignupEmailVerificationId(),
                codeHash,
                codeExpiresAt,
                now
        );
        if (updatedCount == 0)
            throw new SignupEmailVerificationRequiredException();
    }

}
