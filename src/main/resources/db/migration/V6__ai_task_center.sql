-- AI 任务中心：任务、步骤、事件三张表
CREATE TABLE IF NOT EXISTS agent_tasks (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    goal            VARCHAR(4000) NOT NULL,
    constraints_text VARCHAR(2000),
    model_code      VARCHAR(128),
    status          VARCHAR(32)  NOT NULL,
    current_step    INT,
    max_steps       INT,
    final_summary   VARCHAR(8000),
    error_message   VARCHAR(2048),
    created_by      BIGINT,
    created_at      DATETIME,
    updated_at      DATETIME,
    KEY idx_agent_tasks_user (created_by, created_at)
);

CREATE TABLE IF NOT EXISTS agent_task_steps (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    task_id       VARCHAR(36)  NOT NULL,
    seq           INT,
    title         VARCHAR(255) NOT NULL,
    tool          VARCHAR(64),
    input_json    VARCHAR(4000),
    status        VARCHAR(32)  NOT NULL,
    result_text   VARCHAR(8000),
    error_message VARCHAR(2048),
    created_by    BIGINT,
    created_at    DATETIME,
    updated_at    DATETIME,
    KEY idx_agent_task_steps_task (task_id, seq)
);

CREATE TABLE IF NOT EXISTS agent_task_events (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    task_id    VARCHAR(36)  NOT NULL,
    step_id    VARCHAR(36),
    event_type VARCHAR(64)  NOT NULL,
    payload    VARCHAR(4000),
    created_by BIGINT,
    created_at DATETIME,
    KEY idx_agent_task_events_task (task_id, created_at)
);
