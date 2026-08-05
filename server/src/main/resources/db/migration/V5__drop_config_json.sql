-- ============================================================
-- V5__drop_config_json_column.sql
--
-- Permanently removes config_json from cheque_template.
-- Safe to run because:
--   • V4 already migrated all JSON data to template_layout_fields
--   • V4 also preserved the raw JSON in config_json_backup
--
-- Pre-flight check: verify row counts match before applying
--   SELECT ct.id, COUNT(tlf.id) AS field_rows
--   FROM cheque_template ct
--   LEFT JOIN template_layout_fields tlf ON tlf.template_id = ct.id
--   GROUP BY ct.id;
--   Expected: 7 rows per template_id
-- ============================================================

ALTER TABLE cheque_template
    DROP COLUMN IF EXISTS config_json;
