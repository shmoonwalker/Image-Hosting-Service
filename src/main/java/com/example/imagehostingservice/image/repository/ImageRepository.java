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
import java.util.UUID;

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
                    rs.getObject("public_id", UUID.class),
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
                    public_id,
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
                    public_id,
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
    public Optional<Image> findByPublicId(UUID publicId) {
        String sql = """
            SELECT
                id,
                public_id,
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
            WHERE public_id = ?
            """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                publicId
        ).stream().findFirst();
    }

    public List<Image> findAllByOwnerId(
            Long ownerId,
            String query,
            String contentType,
            String color,
            TaggingStatus taggingStatus,
            Boolean isPublic,
            int limit,
            int offset
    ) {
        String taggingStatusValue = taggingStatus == null
                ? null
                : taggingStatus.name();

        String sql = """
                SELECT
                    id,
                    public_id,
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
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'objects', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'tags', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS search_value(value)
                          WHERE search_value.value ILIKE
                                '%' || BTRIM(CAST(? AS text)) || '%'
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR content_type = ?
                  )
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS detected_color(value)
                          WHERE LOWER(detected_color.value) =
                                LOWER(BTRIM(?))
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR tagging_status = CAST(? AS tagging_status)
                  )
                  AND (
                      CAST(? AS boolean) IS NULL
                      OR is_public = CAST(? AS boolean)
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                ownerId,
                query,
                query,
                contentType,
                contentType,
                color,
                color,
                taggingStatusValue,
                taggingStatusValue,
                isPublic,
                isPublic,
                limit,
                offset
        );
    }

    public List<Image> findAllPublic(
            String query,
            String contentType,
            String color,
            TaggingStatus taggingStatus,
            int limit,
            int offset
    ) {
        String taggingStatusValue = taggingStatus == null
                ? null
                : taggingStatus.name();

        String sql = """
                SELECT
                    id,
                    public_id,
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
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'objects', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'tags', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS search_value(value)
                          WHERE search_value.value ILIKE
                                '%' || BTRIM(CAST(? AS text)) || '%'
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR content_type = ?
                  )
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS detected_color(value)
                          WHERE LOWER(detected_color.value) =
                                LOWER(BTRIM(?))
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR tagging_status = CAST(? AS tagging_status)
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                imageRowMapper,
                query,
                query,
                contentType,
                contentType,
                color,
                color,
                taggingStatusValue,
                taggingStatusValue,
                limit,
                offset
        );
    }

    public long countPublicImages(
            String query,
            String contentType,
            String color,
            TaggingStatus taggingStatus
    ) {
        String taggingStatusValue = taggingStatus == null
                ? null
                : taggingStatus.name();

        String sql = """
                SELECT COUNT(*)
                FROM images
                WHERE is_public = true
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'objects', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'tags', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS search_value(value)
                          WHERE search_value.value ILIKE
                                '%' || BTRIM(CAST(? AS text)) || '%'
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR content_type = ?
                  )
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS detected_color(value)
                          WHERE LOWER(detected_color.value) =
                                LOWER(BTRIM(?))
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR tagging_status = CAST(? AS tagging_status)
                  )
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                query,
                query,
                contentType,
                contentType,
                color,
                color,
                taggingStatusValue,
                taggingStatusValue
        );

        return count != null ? count : 0L;
    }

    public Optional<Image> updateVisibilityByPublicId(
            UUID publicId,
            Long ownerId,
            boolean isPublic
    ) {
        String sql = """
            UPDATE images
            SET
                is_public = ?,
                updated_at = now()
            WHERE public_id = ?
              AND owner_id = ?
            RETURNING
                id,
                public_id,
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
                publicId,
                ownerId
        ).stream().findFirst();
    }

    public long countByOwnerId(
            Long ownerId,
            String query,
            String contentType,
            String color,
            TaggingStatus taggingStatus,
            Boolean isPublic
    ) {
        String taggingStatusValue = taggingStatus == null
                ? null
                : taggingStatus.name();

        String sql = """
                SELECT COUNT(*)
                FROM images
                WHERE owner_id = ?
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'objects', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'tags', '[]'::jsonb) ||
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS search_value(value)
                          WHERE search_value.value ILIKE
                                '%' || BTRIM(CAST(? AS text)) || '%'
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR content_type = ?
                  )
                  AND (
                      NULLIF(BTRIM(CAST(? AS text)), '') IS NULL
                      OR EXISTS (
                          SELECT 1
                          FROM jsonb_array_elements_text(
                              COALESCE(ai_tags -> 'colors', '[]'::jsonb)
                          ) AS detected_color(value)
                          WHERE LOWER(detected_color.value) =
                                LOWER(BTRIM(?))
                      )
                  )
                  AND (
                      CAST(? AS text) IS NULL
                      OR tagging_status = CAST(? AS tagging_status)
                  )
                  AND (
                      CAST(? AS boolean) IS NULL
                      OR is_public = CAST(? AS boolean)
                  )
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                ownerId,
                query,
                query,
                contentType,
                contentType,
                color,
                color,
                taggingStatusValue,
                taggingStatusValue,
                isPublic,
                isPublic
        );

        return count != null ? count : 0L;
    }

    public boolean deleteByPublicIdAndOwnerId(
            UUID publicId,
            Long ownerId
    ) {
        String sql = """
                DELETE FROM images
                WHERE public_id = ?
                  AND owner_id = ?
                """;

        int deletedRows = jdbcTemplate.update(
                sql,
                publicId,
                ownerId
        );

        return deletedRows == 1;
    }
}
