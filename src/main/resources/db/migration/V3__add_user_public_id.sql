ALTER TABLE users
    ADD COLUMN public_id uuid NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE users
    ADD CONSTRAINT uq_users_public_id UNIQUE (public_id);