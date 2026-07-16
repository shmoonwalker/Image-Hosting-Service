CREATE TYPE tagging_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
    );

CREATE TABLE users (
                       id bigserial PRIMARY KEY,
                       name varchar(255) NOT NULL,
                       email varchar(255) NOT NULL UNIQUE,
                       password_hash varchar(255) NOT NULL,
                       created_at timestamptz NOT NULL DEFAULT now(),

                       CONSTRAINT chk_users_email_not_blank
                           CHECK (btrim(email) <> '')
);

CREATE TABLE images (
                        id bigserial PRIMARY KEY,

                        owner_id bigint NOT NULL,

                        original_filename varchar(255) NOT NULL,
                        original_storage_key varchar(500) NOT NULL UNIQUE,
                        thumbnail_storage_key varchar(500) UNIQUE,

                        content_type varchar(100) NOT NULL,
                        size_bytes bigint NOT NULL,
                        width integer NOT NULL,
                        height integer NOT NULL,

                        is_public boolean NOT NULL DEFAULT false,

                        ai_tags jsonb,
                        tagging_status tagging_status NOT NULL DEFAULT 'PENDING',

                        created_at timestamptz NOT NULL DEFAULT now(),
                        updated_at timestamptz,

                        CONSTRAINT fk_images_owner
                            FOREIGN KEY (owner_id)
                                REFERENCES users(id)
                                ON DELETE RESTRICT,

                        CONSTRAINT chk_images_content_type
                            CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),

                        CONSTRAINT chk_images_size_bytes
                            CHECK (size_bytes > 0 AND size_bytes <= 10485760),

                        CONSTRAINT chk_images_width
                            CHECK (width > 0),

                        CONSTRAINT chk_images_height
                            CHECK (height > 0)
);

CREATE INDEX idx_images_owner_created_at
    ON images (owner_id, created_at DESC);

CREATE INDEX idx_images_public_created_at
    ON images (is_public, created_at DESC);