package com.kangworld.fantasybaseball.member.service;

import com.kangworld.fantasybaseball.member.service.dto.MemberSignupRequest;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupResponse;

public interface MemberService {

    MemberSignupResponse signup(MemberSignupRequest request);

}
