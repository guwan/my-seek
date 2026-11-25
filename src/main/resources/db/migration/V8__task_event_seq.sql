-- SSE 事件流：任务内递增序号，支持断点续传
ALTER TABLE agent_task_events ADD COLUMN seq BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_agent_task_events_seq ON agent_task_events (task_id, seq);
