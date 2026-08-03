CREATE TABLE user_profile
(
    user_id            BIGINT PRIMARY KEY REFERENCES users (user_id) ON DELETE CASCADE,
    name               VARCHAR(255) NOT NULL,
    gender             VARCHAR(20)  NOT NULL,
    weight_unit        VARCHAR(10)  NOT NULL,
    measurement_unit   VARCHAR(10)  NOT NULL,
    profile_image_path VARCHAR(500),
    created_at         TIMESTAMP    NOT NULL DEFAULT now()
);