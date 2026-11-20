-- 内置 Ollama 运行时配置：单行记录，id 固定为 'default'
CREATE TABLE IF NOT EXISTS ollama_configs (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    base_url            VARCHAR(512) NOT NULL,
    default_chat_model  VARCHAR(128) NOT NULL,
    embedding_model     VARCHAR(128) NOT NULL,
    pull_model_strategy VARCHAR(32)  NOT NULL DEFAULT 'NEVER',
    updated_at          DATETIME     NULL
);
