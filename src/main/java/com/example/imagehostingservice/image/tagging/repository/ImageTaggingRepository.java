package com.example.imagehostingservice.image.tagging.repository;

import com.example.imagehostingservice.exception.ImageTaggingException;
import com.example.imagehostingservice.image.model.ImageTags;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class ImageTaggingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public boolean markProcessing(Long imageId) {
        String sql = """
                UPDATE images
                SET tagging_status = 'PROCESSING',
                    updated_at = now()
                WHERE id = ?
                  AND tagging_status = 'PENDING'
                """;

        return jdbcTemplate.update(sql, imageId) == 1;
    }

    public boolean markCompleted(
            Long imageId,
            ImageTags imageTags
    ) {
        String tagsJson = serialize(imageTags);

        String sql = """
                UPDATE images
                SET ai_tags = CAST(? AS jsonb),
                    tagging_status = 'COMPLETED',
                    updated_at = now()
                WHERE id = ?
                  AND tagging_status = 'PROCESSING'
                """;

        return jdbcTemplate.update(
                sql,
                tagsJson,
                imageId
        ) == 1;
    }

    public boolean markFailed(Long imageId) {
        String sql = """
                UPDATE images
                SET tagging_status = 'FAILED',
                    updated_at = now()
                WHERE id = ?
                  AND tagging_status = 'PROCESSING'
                """;

        return jdbcTemplate.update(sql, imageId) == 1;
    }

    private String serialize(ImageTags imageTags) {
        try {
            return objectMapper.writeValueAsString(imageTags);
        } catch (JacksonException exception) {
            throw new ImageTaggingException(
                    "Could not serialize image tags",
                    exception
            );
        }
    }
}