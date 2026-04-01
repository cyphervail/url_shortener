CREATE TABLE url_mapping (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,

                             short_url VARCHAR(255) NOT NULL,
                             long_url  VARCHAR(2048) NOT NULL,

                             created_at TIMESTAMP NOT NULL,
                             expired_at TIMESTAMP NULL,

                             CONSTRAINT uc_url_mapping_short_url UNIQUE (short_url)
);
