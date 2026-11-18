-- 用户自定义 AI 接口配置表（OpenAI 兼容）
CREATE TABLE IF NOT EXISTS ai_provider_configs (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    name          VARCHAR(32)  NOT NULL,
    display_name  VARCHAR(64)  NOT NULL,
    base_url      VARCHAR(512) NOT NULL,
    api_key       VARCHAR(1024) NOT NULL,
    models_json   TEXT,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NULL,
    updated_at    DATETIME     NULL,
    CONSTRAINT uk_ai_provider_name UNIQUE (name)
);
