CREATE TABLE splits
(
    split_id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT       NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    name     VARCHAR(255) NOT NULL
);

CREATE INDEX idx_splits_owner_id ON splits (owner_id);