package com.kangworld.fantasybaseball.member.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberRole;
import com.kangworld.fantasybaseball.member.model.MemberStatus;

@JdbcTest
@Import(MemberDaoJdbc.class)
class MemberDaoJdbcTest {

    @Autowired
    private MemberDaoJdbc memberDaoJdbc;

    @Test
    void insert는_회원_기본_컬럼을_저장한다() {
        // given
        Member member = buildMember("test1@example.com", "slugger");

        // when
        long memberId = memberDaoJdbc.insert(member);

        Member savedMember = memberDaoJdbc.findByMemberId(memberId).orElseThrow();

        // then
        assertThat(savedMember.getMemberId()).isEqualTo(memberId);
        assertThat(savedMember.getEmail()).isEqualTo("test1@example.com");
        assertThat(savedMember.getPasswordHash()).isEqualTo("{bcrypt}hashed");
        assertThat(savedMember.getDisplayName()).isEqualTo("slugger");
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.USER);
        assertThat(savedMember.getStatus()).isEqualTo(MemberStatus.OK);
        assertThat(savedMember.isEmailVerified()).isFalse();
    }

    @Test
    void findByEmail은_이메일로_회원을_조회한다() {
        // given
        memberDaoJdbc.insert(buildMember("test2@example.com", "pitcher"));

        // when
        Member savedMember = memberDaoJdbc.findByEmail("test2@example.com").orElseThrow();

        // then
        assertThat(savedMember.getDisplayName()).isEqualTo("pitcher");
        assertThat(savedMember.getEmail()).isEqualTo("test2@example.com");
    }

    @Test
    void insert는_이메일_유니크_제약을_지킨다() {
        // given
        memberDaoJdbc.insert(buildMember("dup@example.com", "first"));

        // when then
        // 중복 이메일은 DB 유니크 제약으로 먼저 막는다.
        assertThatThrownBy(() -> memberDaoJdbc.insert(buildMember("dup@example.com", "second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateStatus는_회원_상태를_변경한다() {
        // given
        long memberId = memberDaoJdbc.insert(buildMember("test3@example.com", "cleanup"));
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 7, 12, 0, 0);

        // when
        int updatedCount = memberDaoJdbc.updateStatus(memberId, MemberStatus.DEL, updatedAt);

        Member savedMember = memberDaoJdbc.findByMemberId(memberId).orElseThrow();

        // then
        assertThat(updatedCount).isEqualTo(1);
        assertThat(savedMember.getStatus()).isEqualTo(MemberStatus.DEL);
        assertThat(savedMember.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private Member buildMember(String email, String displayName) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 7, 9, 0, 0);

        // given fixture는 현재 커밋에서 확정된 기본 회원 컬럼만 채운다.
        return Member.builder()
                .email(email)
                .passwordHash("{bcrypt}hashed")
                .displayName(displayName)
                .role(MemberRole.USER)
                .status(MemberStatus.OK)
                .emailVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
