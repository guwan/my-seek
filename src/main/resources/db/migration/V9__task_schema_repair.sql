-- 幂等修复：即使历史库把 V6~V8 标记为已执行但任务表缺失，也能补齐表与字段。
CREATE TABLE IF NOT EXISTS agent_tasks (
    id               VARCHAR(36)   NOT NULL PRIMARY KEY,
    title            VARCHAR(255)  NOT NULL,
    goal             VARCHAR(4000) NOT NULL,
    constraints_text VARCHAR(2000),
    model_code       VARCHAR(128),
    status           VARCHAR(32)   NOT NULL,
    current_step     INT,
    max_steps        INT,
    final_summary    VARCHAR(8000),
    error_message    VARCHAR(2048),
    created_by       BIGINT,
    created_at       DATETIME,
    updated_at       DATETIME,
    KEY idx_agent_tasks_user (created_by, created_at)
);

CREATE TABLE IF NOT EXISTS agent_task_steps (
    id               VARCHAR(36)   NOT NULL PRIMARY KEY,
    task_id          VARCHAR(36)   NOT NULL,
    seq              INT,
    title            VARCHAR(255)  NOT NULL,
    tool             VARCHAR(64),
    input_json       VARCHAR(4000),
    requires_approval BOOLEAN DEFAULT FALSE,
    approved         BOOLEAN DEFAULT FALSE,
    status           VARCHAR(32)   NOT NULL,
    result_text      VARCHAR(8000),
    error_message    VARCHAR(2048),
    created_by       BIGINT,
    created_at       DATETIME,
    updated_at       DATETIME,
    KEY idx_agent_task_steps_task (task_id, seq)
);

CREATE TABLE IF NOT EXISTS agent_task_events (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    seq        BIGINT NOT NULL DEFAULT 0,
    task_id    VARCHAR(36)  NOT NULL,
    step_id    VARCHAR(36),
    event_type VARCHAR(64)  NOT NULL,
    payload    VARCHAR(4000),
    created_by BIGINT,
    created_at DATETIME,
    KEY idx_agent_task_events_task (task_id, created_at),
    KEY idx_agent_task_events_seq (task_id, seq)
);

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'agent_task_steps'
       AND COLUMN_NAME = 'requires_approval') = 0,
    'ALTER TABLE agent_task_steps ADD COLUMN requires_approval BOOLEAN DEFAULT FALSE',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'agent_task_steps'
       AND COLUMN_NAME = 'approved') = 0,
    'ALTER TABLE agent_task_steps ADD COLUMN approved BOOLEAN DEFAULT FALSE',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'agent_task_events'
       AND COLUMN_NAME = 'seq') = 0,
    'ALTER TABLE agent_task_events ADD COLUMN seq BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'agent_task_events'
       AND INDEX_NAME = 'idx_agent_task_events_seq') = 0,
    'CREATE INDEX idx_agent_task_events_seq ON agent_task_events (task_id, seq)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
