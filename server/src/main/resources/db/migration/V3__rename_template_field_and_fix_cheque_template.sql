-- ============================================================
-- V3__rename_template_field_and_fix_cheque_template.sql
--
-- 1. Renames template_field → template_fields (idempotent)
-- 2. Adds account_id and is_default to cheque_template (idempotent)
-- ============================================================

-- 1. Rename table only if old exists and new does NOT exist
SET @rename_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.tables 
           WHERE table_schema = DATABASE() AND table_name = 'template_field')
    AND NOT EXISTS(SELECT 1 FROM information_schema.tables 
                   WHERE table_schema = DATABASE() AND table_name = 'template_fields'),
    'RENAME TABLE template_field TO template_fields',
    'SELECT ''template_field already renamed or missing'' AS msg'
);
PREPARE rename_stmt FROM @rename_sql;
EXECUTE rename_stmt;
DEALLOCATE PREPARE rename_stmt;

-- 2. Add account_id only if missing
SET @add_account_sql = IF(
    NOT EXISTS(SELECT 1 FROM information_schema.columns 
               WHERE table_schema = DATABASE() AND table_name = 'cheque_template' 
                 AND column_name = 'account_id'),
    'ALTER TABLE cheque_template ADD COLUMN account_id BIGINT NULL AFTER height',
    'SELECT ''account_id already exists'' AS msg'
);
PREPARE add_account_stmt FROM @add_account_sql;
EXECUTE add_account_stmt;
DEALLOCATE PREPARE add_account_stmt;

-- 3. Add is_default only if missing
SET @add_default_sql = IF(
    NOT EXISTS(SELECT 1 FROM information_schema.columns 
               WHERE table_schema = DATABASE() AND table_name = 'cheque_template' 
                 AND column_name = 'is_default'),
    'ALTER TABLE cheque_template ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE AFTER account_id',
    'SELECT ''is_default already exists'' AS msg'
);
PREPARE add_default_stmt FROM @add_default_sql;
EXECUTE add_default_stmt;
DEALLOCATE PREPARE add_default_stmt;