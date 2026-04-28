package com.kangworld.fantasybaseball.member.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.kangworld.fantasybaseball.member.dao.MemberDao;
import com.kangworld.fantasybaseball.member.model.Member;
import com.kangworld.fantasybaseball.member.model.MemberRole;
import com.kangworld.fantasybaseball.member.model.MemberStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemberDaoJdbc implements MemberDao {

    private static final RowMapper<Member> MEMBER_ROW_MAPPER = new MemberRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Optional<Member> findByMemberId(long memberId) {
        String sql = """
                select *
                from member
                where member_id = :memberId
                """;

        try {
            Member member = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of("memberId", memberId),
                    MEMBER_ROW_MAPPER
            );

            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        String sql = """
                select *
                from member
                where email = :email
                """;

        try {
            // 로그인 경로에서 바로 재사용할 조회라 email 기준 단건 조회를 기본 DAO에 둔다.
            Member member = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    Map.of("email", email),
                    MEMBER_ROW_MAPPER
            );

            return Optional.ofNullable(member);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public long insert(Member member) {
        String sql = """
                insert into member values (
                    default,
                    :email,
                    :passwordHash,
                    :displayName,
                    :role,
                    :status,
                    :emailVerified,
                    :createdAt,
                    :updatedAt
                )
                """;

        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("email", member.getEmail())
                .addValue("passwordHash", member.getPasswordHash())
                .addValue("displayName", member.getDisplayName())
                .addValue("role", member.getRole().name())
                .addValue("status", member.getStatus().name())
                .addValue("emailVerified", member.isEmailVerified())
                .addValue("createdAt", Timestamp.valueOf(member.getCreatedAt()))
                .addValue("updatedAt", Timestamp.valueOf(member.getUpdatedAt()));

        // 회원 생성 직후 식별자가 필요해서 generated key를 바로 회수한다.
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, parameterSource, keyHolder, new String[]{"member_id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("member insert key is null");
        }

        return key.longValue();
    }

    @Override
    public int updateStatus(long memberId, MemberStatus status, LocalDateTime updatedAt) {
        String sql = """
                update member
                set
                    status = :status,
                    updated_at = :updatedAt
                where member_id = :memberId
                """;

        return namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("memberId", memberId)
                        .addValue("status", status.name())
                        .addValue("updatedAt", Timestamp.valueOf(updatedAt))
        );
    }

    private static class MemberRowMapper implements RowMapper<Member> {

        @Override
        public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
            // DB row는 enum과 시각 타입까지 도메인 모델로 바로 변환.
            return Member.builder()
                    .memberId(rs.getLong("member_id"))
                    .email(rs.getString("email"))
                    .passwordHash(rs.getString("password_hash"))
                    .displayName(rs.getString("display_name"))
                    .role(MemberRole.valueOf(rs.getString("role")))
                    .status(MemberStatus.valueOf(rs.getString("status")))
                    .emailVerified(rs.getBoolean("email_verified"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

    }

}
