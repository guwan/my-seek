-- AI 任务中心：敏感工具执行前的用户审批
ALTER TABLE agent_task_steps ADD COLUMN requires_approval BOOLEAN DEFAULT FALSE;
ALTER TABLE agent_task_steps ADD COLUMN approved BOOLEAN DEFAULT FALSE;
