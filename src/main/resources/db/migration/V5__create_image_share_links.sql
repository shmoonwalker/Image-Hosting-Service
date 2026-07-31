CREATE TABLE image_share_links (
                                   id bigserial PRIMARY KEY,

                                   public_id uuid NOT NULL DEFAULT gen_random_uuid(),
                                   image_id bigint NOT NULL,

                                   token_hash char(64) NOT NULL,
                                   expires_at timestamptz NOT NULL,
                                   revoked_at timestamptz,
                                   created_at timestamptz NOT NULL DEFAULT now(),

                                   CONSTRAINT uq_image_share_links_public_id
                                       UNIQUE (public_id),

                                   CONSTRAINT uq_image_share_links_token_hash
                                       UNIQUE (token_hash),

                                   CONSTRAINT fk_image_share_links_image
                                       FOREIGN KEY (image_id)
                                           REFERENCES images(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT chk_image_share_links_expiration
                                       CHECK (expires_at > created_at),

                                   CONSTRAINT chk_image_share_links_revocation
                                       CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_image_share_links_image_created_at
    ON image_share_links (image_id, created_at DESC);

CREATE INDEX idx_image_share_links_expires_at
    ON image_share_links (expires_at);