package com.example.imagehostingservice.image.repository;

import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.model.ImageTags;
import com.example.imagehostingservice.image.model.TaggingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private ImageTags parseImageTags(ResultSet rs) throws SQLException {
        String json = rs.getString("ai_tags");

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, ImageTags.class);
        } catch (JacksonException exception) {
            throw new SQLException(
                    "Could not parse AI tags for image " + rs.getLong("id"),
                    exception
            );
        }
    }

    private final RowMapper<Image> imageRowMapper = (rs, rowNum) ->
            new Image(
                    rs.getLong("id"),
                    rs.getLong("owner_id"),
                    rs.getString("original_filename"),
                    rs.getString("original_storage_key"),
                    rs.getString("thumbnail_storage_key"),
                    rs.getString("content_type"),
                    rs.getLong("size_bytes"),
                    rs.getInt("width"),
                    rs.getInt("height"),
                    rs.getBoolean("is_public"),
                    parseImageTags(rs),
                    TaggingStatus.valueOf(rs.getString("tagging_status")),
                    rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("updated_at", OffsetDateTime.class)
            );

    public Image save(
            Long ownerId,
            String originalFilename,
            String originalStorageKey,
            String thumbnailStorageKey,
            String contentType,
            Long sizeBytes,
            Integer width,
            Integer height
    ) {
        String sql = """
            INSERT INTO images (
                owner_id,
                original_filename,
                original_storage_key,
                thumbnail_storage_key,
                content_type,
                size_bytes,
                width,
                height
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING
                id,
                owner_id,
                original_filename,
                original_storage_key,
                thumbnail_storage_key,
                content_type,
                size_bytes,
                width,
                height,
                is_public,
                ai_tags,
                tagging_status,
                created_at,
                updated_at
            """;

        return jdbcTemplate.queryForObject(
                sql,
                imageRowMapper,
                ownerId,
                originalFilename,
                originalStorageKey,
                thumbnailStorageKey,
                contentType,
                sizeBytes,
                width,
                height
        );
    }

    public Optional<Image> findById(Long imageId) {
        String sql = """
                SELECT
                    id,
                    owner_id,
                    original_filename,
                    original_storage_key,
                    thumbnail_storage_key,
                    content_type,
                    size_bytes,
                    width,
                    height,
                    is_public,
                    ai_tags,
                    tagging_status,
                    created_at,
                    updated_at
                FROM images
                WHERE id = ?
                """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                imageId
        ).stream().findFirst();
    }

    public List<Image> findAllByOwnerId(
            Long ownerId,
            int limit,
            int offset
    ) {
        String sql = """
            SELECT
                id,
                owner_id,
                original_filename,
                original_storage_key,
                thumbnail_storage_key,
                content_type,
                size_bytes,
                width,
                height,
                is_public,
                ai_tags,
                tagging_status,
                created_at,
                updated_at
            FROM images
            WHERE owner_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            OFFSET ?
            """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                ownerId,
                limit,
                offset
        );
    }

    public List<Image> findAllPublic(int limit, int offset) {
        String sql = """
                SELECT
                    id,
                    owner_id,
                    original_filename,
                    original_storage_key,
                    thumbnail_storage_key,
                    content_type,
                    size_bytes,
                    width,
                    height,
                    is_public,
                    ai_tags,
                    tagging_status,
                    created_at,
                    updated_at
                FROM images
                WHERE is_public = true
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                limit,
                offset
        );
    }

    public long countPublicImages() {
        String sql = """
                SELECT COUNT(*)
                FROM images
                WHERE is_public = true
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class
        );

        return count != null ? count : 0L;
    }

    public Optional<Image> updateVisibility(
            Long imageId,
            Long ownerId,
            boolean isPublic
    ) {
        String sql = """
            UPDATE images
            SET
                is_public = ?,
                updated_at = now()
            WHERE id = ?
              AND owner_id = ?
            RETURNING
                id,
                owner_id,
                original_filename,
                original_storage_key,
                thumbnail_storage_key,
                content_type,
                size_bytes,
                width,
                height,
                is_public,
                ai_tags,
                tagging_status,
                created_at,
                updated_at
            """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                isPublic,
                imageId,
                ownerId
        ).stream().findFirst();
    }

    public long countByOwnerId(Long ownerId) {
        String sql = """
            SELECT COUNT(*)
            FROM images
            WHERE owner_id = ?
            """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                ownerId
        );

        return count != null ? count : 0L;
    }
}