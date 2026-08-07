-- ============================================================
-- V6: Full-text search indexes for Cheque search optimization
-- Must run AFTER V5 performance indexes
-- ============================================================

USE chequeprint_db;

-- Fulltext index for payee name search
CREATE FULLTEXT INDEX idx_cheques_payee_ft ON cheques(payee_name);

-- Fulltext index for cheque number search
CREATE FULLTEXT INDEX idx_cheques_cheque_no_ft ON cheques(cheque_no);

-- Combined fulltext index (alternative to individual ones)
-- DROP the individual ones first if you want to use combined:
-- DROP INDEX idx_cheques_payee_ft ON cheques;
-- DROP INDEX idx_cheques_cheque_no_ft ON cheques;
-- CREATE FULLTEXT INDEX idx_cheques_search_combined ON cheques(payee_name, cheque_no);
