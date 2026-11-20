-- 写作工作台作品表：完整作品树以 JSON 文档保存，按用户隔离
CREATE TABLE IF NOT EXISTS writing_novels (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content_json LONGTEXT     NOT NULL,
    created_at   DATETIME     NULL,
    updated_at   DATETIME     NULL,
    KEY idx_writing_novels_user_id (user_id)
);
