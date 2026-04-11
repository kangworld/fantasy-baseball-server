package com.kangworld.fantasybaseball.auth.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshAccessTokenRequest {

    @NotNull
    private Long memberId;

    @NotBlank
    private String refreshToken;

}
