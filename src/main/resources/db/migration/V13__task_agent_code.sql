-- 任务可选专家（Agent），空表示通用助理。
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_tasks'
       AND COLUMN_NAME = 'agent_code') = 0,
    'ALTER TABLE agent_tasks ADD COLUMN agent_code VARCHAR(64)',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
