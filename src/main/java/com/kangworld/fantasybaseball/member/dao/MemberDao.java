package com.kangworld.fantasybaseball.member.dao;

import java.time.LocalDateTime;
import java.util.Optional;

import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberStatus;

public interface MemberDao {

    Optional<Member> findByMemberId(long memberId);

    Optional<Member> findByEmail(String email);

    long insert(Member member);

    int updateStatus(long memberId, MemberStatus status, LocalDateTime updatedAt);

}
