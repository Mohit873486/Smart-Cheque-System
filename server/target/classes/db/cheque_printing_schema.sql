-- ============================================================
-- MySQL Schema for Cheque Printing System
-- Tables: bank_account, cheque_template, template_field
-- ============================================================

CREATE DATABASE IF NOT EXISTS chequeprint_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chequeprint_db;

-- 1. Table: bank_account
DROP TABLE IF EXISTS template_field;
DROP TABLE IF EXISTS cheque_template;
DROP TABLE IF EXISTS bank_account;

CREATE TABLE bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(35) NOT NULL UNIQUE,
    ifsc VARCHAR(11) NOT NULL,
    branch VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table: cheque_template
CREATE TABLE cheque_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id BIGINT NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    width DOUBLE NOT NULL DEFAULT 203.20,
    height DOUBLE NOT NULL DEFAULT 92.00,
    CONSTRAINT fk_cheque_template_bank
        FOREIGN KEY (bank_id) REFERENCES bank_account(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_cheque_template_bank (bank_id)
);

-- 3. Table: template_field
CREATE TABLE template_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    field_name VARCHAR(50) NOT NULL, -- e.g. 'name', 'amount', 'amount_words', 'date', 'signature'
    x_position DOUBLE NOT NULL,
    y_position DOUBLE NOT NULL,
    font_size INT NOT NULL DEFAULT 12,
    font_family VARCHAR(50) NOT NULL DEFAULT 'Arial',
    CONSTRAINT fk_template_field_template
        FOREIGN KEY (template_id) REFERENCES cheque_template(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_template_field_template (template_id)
);

-- ============================================================
-- SAMPLE DATA INSERTION
-- ============================================================

-- Insert Sample Bank Accounts
INSERT INTO bank_account (id, bank_name, account_number, ifsc, branch) VALUES
(1, 'State Bank of India', '123456789012', 'SBIN0000123', 'Mumbai Main Branch'),
(2, 'HDFC Bank', '987654321098', 'HDFC0000456', 'Delhi Connaught Place Branch'),
(3, 'ICICI Bank', '556677889900', 'ICIC0000789', 'Bandra West Branch'),
(4, 'Axis Bank', '112233445566', 'UTIB0000112', 'MG Road Branch');

-- Insert Sample Cheque Templates
INSERT INTO cheque_template (id, bank_id, template_name, width, height) VALUES
(1, 1, 'SBI CTS-2010 Standard Cheque', 203.20, 92.00),
(2, 2, 'HDFC Corporate Premium Cheque', 203.20, 92.00),
(3, 3, 'ICICI Commercial Cheque', 203.20, 92.00),
(4, 4, 'Axis Bank Business Cheque', 203.20, 92.00);

-- Insert Sample Template Fields
INSERT INTO template_field (template_id, field_name, x_position, y_position, font_size, font_family) VALUES
-- SBI Template Fields (template_id = 1)
(1, 'date', 155.00, 14.00, 12, 'Courier New'),
(1, 'name', 30.00, 28.00, 13, 'Arial'),
(1, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(1, 'amount', 150.00, 45.00, 14, 'Consolas'),
(1, 'signature', 145.00, 72.00, 10, 'Arial'),

-- HDFC Template Fields (template_id = 2)
(2, 'date', 158.00, 12.00, 12, 'Courier New'),
(2, 'name', 32.00, 26.00, 13, 'Arial'),
(2, 'amount_words', 36.00, 40.00, 11, 'Arial'),
(2, 'amount', 152.00, 44.00, 14, 'Consolas'),
(2, 'signature', 148.00, 70.00, 10, 'Arial'),

-- ICICI Template Fields (template_id = 3)
(3, 'date', 154.00, 15.00, 12, 'Courier New'),
(3, 'name', 28.00, 30.00, 13, 'Arial'),
(3, 'amount_words', 33.00, 43.00, 11, 'Arial'),
(3, 'amount', 148.00, 46.00, 14, 'Consolas'),
(3, 'signature', 142.00, 74.00, 10, 'Arial'),

-- Axis Template Fields (template_id = 4)
(4, 'date', 156.00, 13.00, 12, 'Courier New'),
(4, 'name', 31.00, 27.00, 13, 'Arial'),
(4, 'amount_words', 35.00, 41.00, 11, 'Arial'),
(4, 'amount', 151.00, 45.00, 14, 'Consolas'),
(4, 'signature', 146.00, 71.00, 10, 'Arial');
