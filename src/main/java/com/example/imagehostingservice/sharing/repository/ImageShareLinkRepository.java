package com.example.imagehostingservice.sharing.repository;

import com.example.imagehostingservice.sharing.model.ImageShareLink;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ImageShareLinkRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ImageShareLink> shareLinkRowMapper =
            (resultSet, rowNumber) -> new ImageShareLink(
                    resultSet.getLong("id"),
                    resultSet.getObject("public_id", UUID.class),
                    resultSet.getLong("image_id"),
                    resultSet.getString("token_hash"),
                    resultSet.getObject(
                            "expires_at",
                            OffsetDateTime.class
                    ),
                    resultSet.getObject(
                            "revoked_at",
                            OffsetDateTime.class
                    ),
                    resultSet.getObject(
                            "created_at",
                            OffsetDateTime.class
                    )
            );

    public ImageShareLink save(
            Long imageId,
            String tokenHash,
            OffsetDateTime expiresAt
    ) {
        String sql = """
                INSERT INTO image_share_links (
                    image_id,
                    token_hash,
                    expires_at
                )
                VALUES (?, ?, ?)
                RETURNING
                    id,
                    public_id,
                    image_id,
                    token_hash,
                    expires_at,
                    revoked_at,
                    created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                shareLinkRowMapper,
                imageId,
                tokenHash,
                expiresAt
        );
    }

    public List<ImageShareLink> findAllByImageId(Long imageId) {
        String sql = """
                SELECT
                    id,
                    public_id,
                    image_id,
                    token_hash,
                    expires_at,
                    revoked_at,
                    created_at
                FROM image_share_links
                WHERE image_id = ?
                ORDER BY created_at DESC, id DESC
                """;

        return jdbcTemplate.query(
                sql,
                shareLinkRowMapper,
                imageId
        );
    }

    public Optional<ImageShareLink> findActiveByTokenHash(
            String tokenHash
    ) {
        String sql = """
                SELECT
                    id,
                    public_id,
                    image_id,
                    token_hash,
                    expires_at,
                    revoked_at,
                    created_at
                FROM image_share_links
                WHERE token_hash = ?
                  AND revoked_at IS NULL
                  AND expires_at > now()
                """;

        return jdbcTemplate.query(
                sql,
                shareLinkRowMapper,
                tokenHash
        ).stream().findFirst();
    }

    public boolean revokeByPublicIdAndImageId(
            UUID shareLinkId,
            Long imageId
    ) {
        String sql = """
                UPDATE image_share_links
                SET revoked_at = now()
                WHERE public_id = ?
                  AND image_id = ?
                  AND revoked_at IS NULL
                """;

        return jdbcTemplate.update(
                sql,
                shareLinkId,
                imageId
        ) == 1;
    }
}