-- 知识库异步处理：记录重试次数与最近一次处理时间
ALTER TABLE knowledge_documents ADD COLUMN retry_count INT DEFAULT 0;
ALTER TABLE knowledge_documents ADD COLUMN last_attempt_at DATETIME NULL;
