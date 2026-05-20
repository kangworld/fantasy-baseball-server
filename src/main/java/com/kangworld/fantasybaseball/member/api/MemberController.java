package com.kangworld.fantasybaseball.member.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.member.service.MemberService;
import com.kangworld.fantasybaseball.member.service.SignupEmailVerificationService;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupRequest;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupResponse;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationConfirmRequest;
import com.kangworld.fantasybaseball.member.service.dto.SignupEmailVerificationSendRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final SignupEmailVerificationService signupEmailVerificationService;

    @PostMapping("/signup/email-verification")
    @AbuseLimit(policy = "signup-email-verification-send", key = "#request.email", maxAttempts = 5, window = "5m")
    public ApiResponse<Void> sendSignupEmailVerification(@Valid @RequestBody SignupEmailVerificationSendRequest request) {
        signupEmailVerificationService.send(request);

        return ApiResponse.of(HttpStatus.OK, "인증번호를 발송했습니다.", null);
    }

    @PostMapping("/signup/email-verification/confirm")
    public ApiResponse<Void> confirmSignupEmailVerification(@Valid @RequestBody SignupEmailVerificationConfirmRequest request) {
        signupEmailVerificationService.confirm(request);

        return ApiResponse.of(HttpStatus.OK, "이메일 인증이 완료되었습니다.", null);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberSignupResponse> signup(@Valid @RequestBody MemberSignupRequest request) {
        return ApiResponse.of(HttpStatus.CREATED, "회원가입이 완료되었습니다.", memberService.signup(request));
    }

}
