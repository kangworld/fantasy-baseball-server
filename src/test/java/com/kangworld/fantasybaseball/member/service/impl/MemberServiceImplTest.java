package com.kangworld.fantasybaseball.member.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.exception.MemberEmailAlreadyExistsException;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.service.MemberService;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupRequest;
import com.kangworld.fantasybaseball.member.service.dto.MemberSignupResponse;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from member", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MemberServiceImplTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberDao memberDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void signup은_비밀번호를_해시로_저장한다() {
        // given
        MemberSignupRequest request = buildRequest("service@example.com", "plain-password", "cleanup01");

        // when
        MemberSignupResponse response = memberService.signup(request);
        Member savedMember = memberDao.findByMemberId(response.getMemberId()).orElseThrow();

        // then
        assertThat(savedMember.getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(passwordEncoder.matches("plain-password", savedMember.getPasswordHash())).isTrue();
    }

    @Test
    void signup은_중복_이메일이면_예외를_던진다() {
        // given
        MemberSignupRequest request = buildRequest("dup-service@example.com", "plain-password", "cleanup01");
        memberService.signup(request);

        // when then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(MemberEmailAlreadyExistsException.class);
    }

    private MemberSignupRequest buildRequest(String email, String password, String displayName) {
        return new MemberSignupRequest(email, password, displayName);
    }

}
