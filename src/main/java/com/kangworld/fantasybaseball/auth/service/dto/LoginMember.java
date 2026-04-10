package com.kangworld.fantasybaseball.auth.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginMember {

    private Long memberId;
    private String email;
    private String displayName;

}
