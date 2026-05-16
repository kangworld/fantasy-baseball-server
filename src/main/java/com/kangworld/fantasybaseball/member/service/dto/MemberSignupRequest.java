package com.kangworld.fantasybaseball.member.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String verificationCode;

    @NotBlank
    private String password;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$")
    private String displayName;

}
