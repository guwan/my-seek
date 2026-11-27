-- 并行子步骤：相同 parallel_group 且连续的步骤同时执行。
-- 使用 information_schema 动态判断，兼容历史库与 repeatable 场景。
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_task_steps'
       AND COLUMN_NAME = 'parallel_group') = 0,
    'ALTER TABLE agent_task_steps ADD COLUMN parallel_group VARCHAR(64)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
