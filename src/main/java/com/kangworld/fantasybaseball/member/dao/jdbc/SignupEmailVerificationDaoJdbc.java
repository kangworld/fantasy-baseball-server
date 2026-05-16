package com.kangworld.fantasybaseball.member.dao.jdbc;

import com.kangworld.fantasybaseball.member.dao.SignupEmailVerificationDao;
import com.kangworld.fantasybaseball.member.model.SignupEmailVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SignupEmailVerificationDaoJdbc implements SignupEmailVerificationDao {

    private static final RowMapper<SignupEmailVerification> ROW_MAPPER = new SignupEmailVerificationRowMapper();

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Optional<SignupEmailVerification> findActiveByEmail(String email, LocalDateTime now) {
        String sql = """
                select *
                from signup_email_verification
                where email = :email
                  and consumed_at is null
                  and (code_expires_at > :now or verified_expires_at > :now)
                order by created_at desc
                limit 1
                """;

        return findOne(sql, email, now);
    }

    @Override
    public Optional<SignupEmailVerification> findConfirmableByEmail(String email, LocalDateTime now) {
        String sql = """
                select *
                from signup_email_verification
                where email = :email
                  and consumed_at is null
                  and code_expires_at > :now
                order by created_at desc
                limit 1
                """;

        return findOne(sql, email, now);
    }

    @Override
    public Optional<SignupEmailVerification> findVerifiedForSignup(String email, LocalDateTime now) {
        String sql = """
                select *
                from signup_email_verification
                where email = :email
                  and consumed_at is null
                  and verified_at is not null
                  and verified_expires_at > :now
                order by created_at desc
                limit 1
                """;

        return findOne(sql, email, now);
    }

    private Optional<SignupEmailVerification> findOne(String sql, String email, LocalDateTime now) {
        try {
            SignupEmailVerification verification = namedParameterJdbcTemplate.queryForObject(
                    sql,
                    new MapSqlParameterSource()
                            .addValue("email", email)
                            .addValue("now", Timestamp.valueOf(now)),
                    ROW_MAPPER
            );

            return Optional.ofNullable(verification);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public long insert(String email, String codeHash, LocalDateTime codeExpiresAt, LocalDateTime now) {
        String sql = """
                insert into signup_email_verification values (
                    default,
                    :email,
                    :codeHash,
                    :codeExpiresAt,
                    null,
                    null,
                    null,
                    :createdAt,
                    :updatedAt
                )
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("email", email)
                        .addValue("codeHash", codeHash)
                        .addValue("codeExpiresAt", Timestamp.valueOf(codeExpiresAt))
                        .addValue("createdAt", Timestamp.valueOf(now))
                        .addValue("updatedAt", Timestamp.valueOf(now)),
                keyHolder,
                new String[]{"signup_email_verification_id"}
        );

        Number key = keyHolder.getKey();
        if (key == null)
            throw new IllegalStateException("signup email verification insert key is null");

        return key.longValue();
    }

    @Override
    public int updateCode(long signupEmailVerificationId, String codeHash, LocalDateTime codeExpiresAt, LocalDateTime now) {
        String sql = """
                update signup_email_verification
                set
                    code_hash = :codeHash,
                    code_expires_at = :codeExpiresAt,
                    verified_at = null,
                    verified_expires_at = null,
                    updated_at = :updatedAt
                where signup_email_verification_id = :signupEmailVerificationId
                  and consumed_at is null
                """;

        return namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("signupEmailVerificationId", signupEmailVerificationId)
                        .addValue("codeHash", codeHash)
                        .addValue("codeExpiresAt", Timestamp.valueOf(codeExpiresAt))
                        .addValue("updatedAt", Timestamp.valueOf(now))
        );
    }

    @Override
    public int verify(long signupEmailVerificationId, LocalDateTime verifiedAt, LocalDateTime verifiedExpiresAt) {
        String sql = """
                update signup_email_verification
                set
                    verified_at = :verifiedAt,
                    verified_expires_at = :verifiedExpiresAt,
                    updated_at = :updatedAt
                where signup_email_verification_id = :signupEmailVerificationId
                  and consumed_at is null
                """;

        return namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("signupEmailVerificationId", signupEmailVerificationId)
                        .addValue("verifiedAt", Timestamp.valueOf(verifiedAt))
                        .addValue("verifiedExpiresAt", Timestamp.valueOf(verifiedExpiresAt))
                        .addValue("updatedAt", Timestamp.valueOf(verifiedAt))
        );
    }

    @Override
    public int consume(long signupEmailVerificationId, LocalDateTime consumedAt) {
        String sql = """
                update signup_email_verification
                set
                    consumed_at = :consumedAt,
                    updated_at = :updatedAt
                where signup_email_verification_id = :signupEmailVerificationId
                  and consumed_at is null
                """;

        return namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("signupEmailVerificationId", signupEmailVerificationId)
                        .addValue("consumedAt", Timestamp.valueOf(consumedAt))
                        .addValue("updatedAt", Timestamp.valueOf(consumedAt))
        );
    }

    private static class SignupEmailVerificationRowMapper implements RowMapper<SignupEmailVerification> {

        @Override
        public SignupEmailVerification mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SignupEmailVerification.builder()
                    .signupEmailVerificationId(rs.getLong("signup_email_verification_id"))
                    .email(rs.getString("email"))
                    .codeHash(rs.getString("code_hash"))
                    .codeExpiresAt(rs.getTimestamp("code_expires_at").toLocalDateTime())
                    .verifiedAt(toLocalDateTime(rs, "verified_at"))
                    .verifiedExpiresAt(toLocalDateTime(rs, "verified_expires_at"))
                    .consumedAt(toLocalDateTime(rs, "consumed_at"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build();
        }

        private LocalDateTime toLocalDateTime(ResultSet rs, String columnName) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(columnName);
            if (timestamp == null)
                return null;

            return timestamp.toLocalDateTime();
        }

    }

}
