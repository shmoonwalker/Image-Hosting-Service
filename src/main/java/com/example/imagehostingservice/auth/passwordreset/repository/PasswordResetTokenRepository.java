package com.example.imagehostingservice.auth.passwordreset.repository;

import com.example.imagehostingservice.auth.passwordreset.model.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PasswordResetToken> tokenRowMapper =
            (resultSet, rowNumber) -> new PasswordResetToken(
                    resultSet.getLong("id"),
                    resultSet.getLong("user_id"),
                    resultSet.getString("token_hash"),
                    resultSet.getObject(
                            "expires_at",
                            OffsetDateTime.class
                    ),
                    resultSet.getObject(
                            "used_at",
                            OffsetDateTime.class
                    ),
                    resultSet.getObject(
                            "created_at",
                            OffsetDateTime.class
                    )
            );

    public void invalidateUnusedByUserId(Long userId) {
        String sql = """
                UPDATE password_reset_tokens
                SET used_at = now()
                WHERE user_id = ?
                  AND used_at IS NULL
                """;

        jdbcTemplate.update(sql, userId);
    }

    public PasswordResetToken save(
            Long userId,
            String tokenHash,
            OffsetDateTime expiresAt
    ) {
        String sql = """
                INSERT INTO password_reset_tokens (
                    user_id,
                    token_hash,
                    expires_at
                )
                VALUES (?, ?, ?)
                RETURNING
                    id,
                    user_id,
                    token_hash,
                    expires_at,
                    used_at,
                    created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                tokenRowMapper,
                userId,
                tokenHash,
                expiresAt
        );
    }

    public Optional<PasswordResetToken> findValidByTokenHash(
            String tokenHash
    ) {
        String sql = """
                SELECT
                    id,
                    user_id,
                    token_hash,
                    expires_at,
                    used_at,
                    created_at
                FROM password_reset_tokens
                WHERE token_hash = ?
                  AND used_at IS NULL
                  AND expires_at > now()
                """;

        return jdbcTemplate.query(
                sql,
                tokenRowMapper,
                tokenHash
        ).stream().findFirst();
    }

    public boolean markUsed(Long tokenId) {
        String sql = """
                UPDATE password_reset_tokens
                SET used_at = now()
                WHERE id = ?
                  AND used_at IS NULL
                  AND expires_at > now()
                """;

        return jdbcTemplate.update(sql, tokenId) == 1;
    }
}