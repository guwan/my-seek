-- WorkBuddy 风格目录：专家（Agent）/ 技能（Skill）/ 连接器（Connector）
-- 内置项由 BuiltinCatalogInitializer 幂等写入；用户自定义项按 created_by 隔离。
CREATE TABLE IF NOT EXISTS agent_definitions (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    code           VARCHAR(64)  NOT NULL,
    name           VARCHAR(128) NOT NULL,
    description    TEXT,
    icon           VARCHAR(64),
    system_prompt  TEXT,
    skills_json    TEXT,
    model_code     VARCHAR(128),
    builtin        BOOLEAN DEFAULT FALSE,
    enabled        BOOLEAN DEFAULT TRUE,
    created_by     BIGINT,
    created_at     DATETIME,
    updated_at     DATETIME,
    CONSTRAINT uk_agent_definition_code_user UNIQUE (code, created_by),
    KEY idx_agent_definition_user (created_by, enabled)
);

CREATE TABLE IF NOT EXISTS skill_definitions (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    icon            VARCHAR(64),
    prompt_template TEXT,
    tools_json      TEXT,
    builtin         BOOLEAN DEFAULT FALSE,
    enabled         BOOLEAN DEFAULT TRUE,
    created_by      BIGINT,
    created_at      DATETIME,
    updated_at      DATETIME,
    CONSTRAINT uk_skill_definition_code_user UNIQUE (code, created_by),
    KEY idx_skill_definition_user (created_by, enabled)
);

CREATE TABLE IF NOT EXISTS connector_definitions (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    code          VARCHAR(64)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    description   TEXT,
    icon          VARCHAR(64),
    type          VARCHAR(32)  NOT NULL,
    tools_json    TEXT,
    config_json   TEXT,
    builtin       BOOLEAN DEFAULT FALSE,
    enabled       BOOLEAN DEFAULT TRUE,
    created_by    BIGINT,
    created_at    DATETIME,
    updated_at    DATETIME,
    CONSTRAINT uk_connector_definition_code_user UNIQUE (code, created_by),
    KEY idx_connector_definition_user (created_by, enabled)
);
