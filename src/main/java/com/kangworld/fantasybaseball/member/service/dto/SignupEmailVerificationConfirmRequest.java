package com.kangworld.fantasybaseball.member.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupEmailVerificationConfirmRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

}
