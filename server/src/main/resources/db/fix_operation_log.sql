-- ============================================
-- 修复操作日志表缺失字段
-- 数据库: easy_erp
-- ============================================

USE easy_erp;

-- 检查并添加 request_method 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'easy_erp' AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'request_method');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN request_method VARCHAR(20) COMMENT ''请求方法'' AFTER ip',
    'SELECT ''request_method already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 request_url 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'easy_erp' AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'request_url');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN request_url VARCHAR(500) COMMENT ''请求URL'' AFTER request_method',
    'SELECT ''request_url already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 request_params 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'easy_erp' AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'request_params');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN request_params TEXT COMMENT ''请求参数'' AFTER request_url',
    'SELECT ''request_params already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 real_name 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'easy_erp' AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'real_name');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN real_name VARCHAR(100) COMMENT ''用户真实姓名'' AFTER username',
    'SELECT ''real_name already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 error_message 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'easy_erp' AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'error_message');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN error_message TEXT COMMENT ''错误信息'' AFTER result',
    'SELECT ''error_message already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 response_time 字段
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'easy_erp' AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'response_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN response_time BIGINT COMMENT ''响应时间(毫秒)'' AFTER error_message',
    'SELECT ''response_time already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration completed successfully!' AS result;
