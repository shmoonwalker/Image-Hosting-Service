# Database ERD

The database uses numeric IDs internally and UUIDs for resources exposed through the API.

```mermaid
erDiagram
    USERS ||--o{ IMAGES : owns
    IMAGES ||--o{ IMAGE_SHARE_LINKS : has

    USERS {
        BIGINT id PK
        UUID public_id UK
        VARCHAR name
        VARCHAR email UK
        VARCHAR password_hash
        TIMESTAMPTZ created_at
    }

    IMAGES {
        BIGINT id PK
        UUID public_id UK
        BIGINT owner_id FK
        VARCHAR original_filename
        VARCHAR original_storage_key UK
        VARCHAR thumbnail_storage_key UK
        VARCHAR content_type
        BIGINT size_bytes
        INTEGER width
        INTEGER height
        BOOLEAN is_public
        JSONB ai_tags
        TAGGING_STATUS tagging_status
        TIMESTAMPTZ tagging_started_at
        TIMESTAMPTZ tagging_completed_at
        BIGINT tagging_duration_ms
        TEXT tagging_error
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    IMAGE_SHARE_LINKS {
        BIGINT id PK
        UUID public_id UK
        BIGINT image_id FK
        VARCHAR token_hash UK
        TIMESTAMPTZ expires_at
        TIMESTAMPTZ revoked_at
        TIMESTAMPTZ created_at
    }
```

Spring Session JDBC manages its session tables separately, so they are not included in the domain ERD.