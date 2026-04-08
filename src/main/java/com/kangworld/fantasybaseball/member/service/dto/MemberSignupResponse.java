package com.kangworld.fantasybaseball.member.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberSignupResponse {

    private Long memberId;
    private String email;
    private String displayName;

}
