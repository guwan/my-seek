-- 知识库文档表：生产环境 ddl-auto=validate，全新库需要建表脚本兜底
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id            VARCHAR(36)   NOT NULL PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    original_name VARCHAR(255),
    type          VARCHAR(64)   NOT NULL,
    size          BIGINT        NOT NULL,
    content       LONGTEXT,
    storage_path  VARCHAR(1024),
    status        VARCHAR(32)   NOT NULL,
    error_message VARCHAR(2048),
    chunk_count   INT           DEFAULT 0,
    created_at    DATETIME,
    updated_at    DATETIME,
    created_by    BIGINT,
    KEY idx_knowledge_documents_status (status),
    KEY idx_knowledge_documents_created_at (created_at),
    KEY idx_knowledge_documents_created_by (created_by)
);

-- 按用户分页查询的联合索引；用动态 SQL 避免重复创建报错
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_documents'
      AND INDEX_NAME = 'idx_knowledge_documents_user_created'
);
SET @add_idx = IF(@idx_exists = 0,
    'CREATE INDEX idx_knowledge_documents_user_created ON knowledge_documents (created_by, created_at)',
    'SELECT 1');
PREPARE stmt FROM @add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 说明：历史数据中 created_by 为 NULL 的文档不会归属任何用户，
-- 按用户隔离后将不再可见。如需保留，请由管理员手工分配到对应用户。
