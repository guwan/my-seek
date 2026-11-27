-- 将任务中心的长文本字段改为 TEXT，修复 MariaDB/MySQL 行大小超限（Error 1118）。
-- 使用 information_schema 动态判断，重复执行或列已是 TEXT 时跳过。
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tasks'
       AND COLUMN_NAME = 'goal' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_tasks MODIFY COLUMN goal TEXT NOT NULL', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tasks'
       AND COLUMN_NAME = 'constraints_text' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_tasks MODIFY COLUMN constraints_text TEXT', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tasks'
       AND COLUMN_NAME = 'final_summary' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_tasks MODIFY COLUMN final_summary TEXT', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_task_steps'
       AND COLUMN_NAME = 'input_json' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_task_steps MODIFY COLUMN input_json TEXT', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_task_steps'
       AND COLUMN_NAME = 'result_text' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_task_steps MODIFY COLUMN result_text TEXT', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_task_events'
       AND COLUMN_NAME = 'payload' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_task_events MODIFY COLUMN payload TEXT', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tasks'
       AND COLUMN_NAME = 'error_message' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_tasks MODIFY COLUMN error_message VARCHAR(1024)', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_task_steps'
       AND COLUMN_NAME = 'error_message' AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE agent_task_steps MODIFY COLUMN error_message VARCHAR(1024)', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
