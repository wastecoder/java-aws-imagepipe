-- The API owns the schema. The worker reaches these same rows through plain JDBC, so any change
-- here is a change to the worker's contract too.
CREATE TABLE images (
    id           uuid         PRIMARY KEY,
    status       varchar(16)  NOT NULL,
    content_type varchar(64)  NOT NULL,
    size_bytes   bigint       NOT NULL,
    filename     varchar(255),
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL
);

-- The primary key is the natural pair, not a surrogate: a variant has no identity beyond the image
-- it belongs to and its size. That is what lets the worker write
-- INSERT ... ON CONFLICT (image_id, size) DO UPDATE and be idempotent by construction, which is the
-- same lesson as the deterministic S3 key, expressed in the schema.
CREATE TABLE image_variants (
    image_id uuid         NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    size     varchar(16)  NOT NULL,
    s3_key   varchar(255) NOT NULL,
    width    integer      NOT NULL,
    height   integer      NOT NULL,
    bytes    bigint       NOT NULL,
    PRIMARY KEY (image_id, size)
);
