ALTER TABLE images
    ADD COLUMN public_id uuid NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE images
    ADD CONSTRAINT uq_images_public_id UNIQUE (public_id);