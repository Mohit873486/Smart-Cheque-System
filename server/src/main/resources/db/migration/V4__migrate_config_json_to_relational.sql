-- ============================================================
-- V4__migrate_config_json_to_template_layout_fields.sql
--
-- Normalises the config_json LONGTEXT column in cheque_template
-- into a proper relational table: template_layout_fields.
--
-- Steps:
--   1. Create template_layout_fields table
--   2. Migrate existing JSON data (7 LayoutField rows per template)
--   3. Back up config_json to config_json_backup before dropping
--   4. Drop config_json from cheque_template
-- ============================================================

-- ── 1. Create the new normalized table ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS template_layout_fields (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id  BIGINT       NOT NULL,
    field_name   VARCHAR(30)  NOT NULL,   -- matches LayoutField enum name
    x_ratio      DOUBLE NOT NULL DEFAULT 0.0,
    y_ratio      DOUBLE NOT NULL DEFAULT 0.0,
    width_ratio  DOUBLE NOT NULL DEFAULT 0.0,
    height_ratio DOUBLE NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_tlf_template
        FOREIGN KEY (template_id) REFERENCES cheque_template(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    -- One row per field per template
    UNIQUE KEY uq_tlf_template_field (template_id, field_name),
    INDEX idx_tlf_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 2. Back up config_json before dropping ───────────────────────────────────
-- MySQL does not support IF NOT EXISTS with ALTER TABLE ADD COLUMN, so we
-- gate the statement on information_schema and execute it only when needed.
SET @config_json_backup_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cheque_template'
      AND column_name = 'config_json_backup'
);

SET @config_json_backup_sql := IF(
    @config_json_backup_exists = 0,
    'ALTER TABLE cheque_template ADD COLUMN config_json_backup LONGTEXT NULL COMMENT ''config_json content preserved from V4 migration''',
    'SELECT 1'
);

PREPARE config_json_backup_stmt FROM @config_json_backup_sql;
EXECUTE config_json_backup_stmt;
DEALLOCATE PREPARE config_json_backup_stmt;

UPDATE cheque_template
SET config_json_backup = config_json
WHERE id > 0
  AND config_json IS NOT NULL
  AND config_json <> ''
  AND config_json <> '{}';

-- ── 3. Migrate JSON → relational rows ────────────────────────────────────────
-- For each template that has valid JSON, extract each of the 7 LayoutFields.
-- JSON path: $.fieldPositions.FIELD_NAME.{xRatio|yRatio|widthRatio|heightRatio}
-- INSERT IGNORE skips templates that already have rows (idempotent).

INSERT IGNORE INTO template_layout_fields
    (template_id, field_name, x_ratio, y_ratio, width_ratio, height_ratio)
SELECT
    ct.id,
    fields.field_name,
    COALESCE(
        JSON_EXTRACT(ct.config_json, CONCAT('$.fieldPositions.', fields.field_name, '.xRatio')),
        fields.default_x),
    COALESCE(
        JSON_EXTRACT(ct.config_json, CONCAT('$.fieldPositions.', fields.field_name, '.yRatio')),
        fields.default_y),
    COALESCE(
        JSON_EXTRACT(ct.config_json, CONCAT('$.fieldPositions.', fields.field_name, '.widthRatio')),
        fields.default_w),
    COALESCE(
        JSON_EXTRACT(ct.config_json, CONCAT('$.fieldPositions.', fields.field_name, '.heightRatio')),
        fields.default_h)
FROM cheque_template ct
-- Cross-join against a values table of all 7 LayoutField names + their defaults
JOIN (
    SELECT 'BANK_LOGO'     AS field_name, 0.06 AS default_x, 0.08 AS default_y, 0.30 AS default_w, 0.10 AS default_h UNION ALL
    SELECT 'DATE',                         0.78,              0.08,              0.18,              0.08             UNION ALL
    SELECT 'PAYEE',                        0.16,              0.28,              0.78,              0.08             UNION ALL
    SELECT 'AMOUNT_NUMBER',                0.76,              0.42,              0.18,              0.10             UNION ALL
    SELECT 'AMOUNT_WORDS',                 0.16,              0.40,              0.54,              0.16             UNION ALL
    SELECT 'SIGNATURE',                    0.72,              0.65,              0.22,              0.16             UNION ALL
    SELECT 'MICR',                         0.50,              0.88,              0.50,              0.08
) AS fields;

-- ── 4. Drop config_json is deferred to V5 ────────────────────────────────────
--    Run V5 after verifying that template_layout_fields data looks correct.
