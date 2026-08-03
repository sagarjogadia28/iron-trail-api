ALTER TABLE user_profile
    ADD COLUMN active_split_id BIGINT REFERENCES splits (split_id) ON DELETE SET NULL;