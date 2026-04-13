package com.kangworld.fantasybaseball.auth.security;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthenticatedMember {

    private Long memberId;
    private String role;

}
