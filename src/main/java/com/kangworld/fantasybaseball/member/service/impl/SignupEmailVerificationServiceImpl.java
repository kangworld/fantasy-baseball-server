package com.kangworld.fantasybaseball.member.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.member.config.MemberProperties;
import com.kangworld.fantasybaseball.member.dao.SignupEmailVerificationDao;
import com.kangworld.fantasybaseball.member.exception.SignupEmailVerificationRequiredException;
import com.kangworld.fantasybaseball.member.mail.MemberMailSender;
import com.kangworld.fantasybaseball.member.model.SignupEmailVerification;
import com.kangworld.fantasybaseball.member.service.SignupEmailVerificationService;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationConfirmRequest;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationSendRequest;
import com.kangworld.rediscore.lock.annotation.RedisLock;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupEmailVerificationServiceImpl implements SignupEmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SignupEmailVerificationDao signupEmailVerificationDao;
    private final PasswordEncoder passwordEncoder;
    private final MemberMailSender memberMailSender;
    private final FantasyTime fantasyTime;
    private final SignupEmailVerificationPersistenceService persistenceService;
    private final MemberProperties memberProperties;

    @Override
    @RedisLock(name = "signup-email-verification", key = "#request.email")
    public void send(SignupEmailVerificationSendRequest request) {
        LocalDateTime now = fantasyTime.getDateTime();
        String code = createCode();
        String codeHash = hash(code);
        long expirationMinutes = memberProperties.signup().emailVerification().codeExpirationMinutes();
        LocalDateTime codeExpiresAt = now.plus(Duration.ofMinutes(expirationMinutes));

        // SMTP 지연 동안 DB 트랜잭션을 점유하지 않도록 인증번호 저장을 먼저 커밋한다.
        persistenceService.saveCode(request.getEmail(), codeHash, codeExpiresAt, now);
        // 발송 실패 후 재요청하면 동일 이메일의 저장된 인증번호를 새 코드로 교체한다.
        memberMailSender.sendSignupVerificationCode(request.getEmail(), code);
    }

    @Override
    @Transactional
    @RedisLock(name = "signup-email-verification", key = "#request.email")
    public void confirm(SignupEmailVerificationConfirmRequest request) {
        LocalDateTime now = fantasyTime.getDateTime();
        SignupEmailVerification verification = getConfirmableVerification(request.getEmail(), now);

        if (!matches(request.getCode(), verification.getCodeHash()))
            throw new SignupEmailVerificationRequiredException();

        int updatedCount = signupEmailVerificationDao.verify(
                verification.getSignupEmailVerificationId(),
                now,
                now.plus(Duration.ofMinutes(
                        memberProperties.signup().emailVerification().verifiedExpirationMinutes()))
        );

        if (updatedCount == 0)
            throw new SignupEmailVerificationRequiredException();
    }

    @Override
    @Transactional
    @RedisLock(name = "signup-email-verification", key = "#email")
    public void consumeVerifiedEmail(String email, String verificationCode, LocalDateTime now) {
        SignupEmailVerification verification = getVerifiedForSignup(email, now);

        // 최종 가입에서도 인증번호를 다시 확인한다. 프론트의 인증 완료 상태만으로는 가입을 허용하지 않는다.
        if (!matches(verificationCode, verification.getCodeHash()))
            throw new SignupEmailVerificationRequiredException();

        // 회원 생성에 사용된 인증 row는 즉시 소비 처리해서 같은 인증번호를 재사용하지 못하게 한다.
        int updatedCount = signupEmailVerificationDao.consume(verification.getSignupEmailVerificationId(), now);
        if (updatedCount == 0)
            throw new SignupEmailVerificationRequiredException();
    }

    private SignupEmailVerification getConfirmableVerification(String email, LocalDateTime now) {
        return signupEmailVerificationDao.findConfirmableByEmail(email, now)
                .orElseThrow(SignupEmailVerificationRequiredException::new);
    }

    private SignupEmailVerification getVerifiedForSignup(String email, LocalDateTime now) {
        return signupEmailVerificationDao.findVerifiedForSignup(email, now)
                .orElseThrow(SignupEmailVerificationRequiredException::new);
    }

    private String createCode() {
        return "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
    }

    private String hash(String code) {
        return passwordEncoder.encode(code);
    }

    private boolean matches(String code, String codeHash) {
        return passwordEncoder.matches(code, codeHash);
    }

}
