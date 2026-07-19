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
                content_type,
                size_bytes,
                width,
                height
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
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
                contentType,
                sizeBytes,
                width,
                height
        );
    }
}