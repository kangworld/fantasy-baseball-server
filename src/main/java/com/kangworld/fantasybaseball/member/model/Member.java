package com.kangworld.fantasybaseball.member.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Member {

    private Long memberId;
    private String email;
    private String passwordHash;
    private String displayName;
    private MemberRole role;
    private MemberStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
