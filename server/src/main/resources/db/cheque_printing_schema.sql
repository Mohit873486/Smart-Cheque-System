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
    account_holder_name VARCHAR(150) NOT NULL,
    ifsc VARCHAR(11) NOT NULL,
    branch VARCHAR(100),
    signature_path VARCHAR(255),
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

-- Insert Sample Bank Accounts (IDs: 1-4, 6, 7, 8, 9, 11)
INSERT INTO bank_account (id, bank_name, account_number, account_holder_name, ifsc, branch) VALUES
(1, 'State Bank of India', '123456789012', 'Acme Corp Operating Account', 'SBIN0000123', 'Mumbai Main Branch'),
(2, 'HDFC Bank', '987654321098', 'Acme Corp Payroll Account', 'HDFC0000456', 'Delhi Connaught Place Branch'),
(3, 'ICICI Bank', '556677889900', 'Acme Corp Reserve Account', 'ICIC0000789', 'Bandra West Branch'),
(4, 'Axis Bank', '112233445566', 'Acme Corp Vendor Account', 'UTIB0000112', 'MG Road Branch'),
(6, 'Kotak Mahindra Bank', '667788990011', 'Acme Corp Tax Account', 'KKBK0000667', 'Nariman Point Branch'),
(7, 'Punjab National Bank', '778899001122', 'Acme Corp Treasury Account', 'PUNB0000778', 'Connaught Place Branch'),
(8, 'Bank of Baroda', '889900112233', 'Acme Corp Escrow Account', 'BARB0000889', 'Alkapuri Branch'),
(9, 'Canara Bank', '990011223344', 'Acme Corp Dividend Account', 'CNRB0000990', 'Town Hall Branch'),
(11, 'IndusInd Bank', '110022334455', 'Acme Corp Operations Account', 'INDB0000111', 'Cyber City Branch');

-- Insert Sample Cheque Templates linked to Bank IDs
INSERT INTO cheque_template (id, bank_id, template_name, width, height) VALUES
(1, 1, 'SBI CTS-2010 Standard Cheque', 203.20, 92.00),
(2, 2, 'HDFC Corporate Premium Cheque', 203.20, 92.00),
(3, 3, 'ICICI Commercial Cheque', 203.20, 92.00),
(4, 4, 'Axis Bank Business Cheque', 203.20, 92.00),
(6, 6, 'Kotak Corporate Cheque Template', 203.20, 92.00),
(7, 7, 'PNB Standard Cheque Template', 203.20, 92.00),
(8, 8, 'BOB Business Cheque Template', 203.20, 92.00),
(9, 9, 'Canara Bank Cheque Template', 203.20, 92.00),
(11, 11, 'IndusInd Commercial Template', 203.20, 92.00);

-- Insert Sample Template Fields
INSERT INTO template_field (template_id, field_name, x_position, y_position, font_size, font_family) VALUES
-- SBI Template Fields (template_id = 1)
(1, 'date', 155.00, 14.00, 12, 'Courier New'),
(1, 'name', 30.00, 28.00, 13, 'Arial'),
(1, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(1, 'amount', 150.00, 45.00, 14, 'Consolas'),
(1, 'signature', 145.00, 72.00, 10, 'Arial'),

-- Kotak Template Fields (template_id = 6)
(6, 'date', 155.00, 14.00, 12, 'Courier New'),
(6, 'name', 30.00, 28.00, 13, 'Arial'),
(6, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(6, 'amount', 150.00, 45.00, 14, 'Consolas'),
(6, 'signature', 145.00, 72.00, 10, 'Arial'),

-- PNB Template Fields (template_id = 7)
(7, 'date', 155.00, 14.00, 12, 'Courier New'),
(7, 'name', 30.00, 28.00, 13, 'Arial'),
(7, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(7, 'amount', 150.00, 45.00, 14, 'Consolas'),
(7, 'signature', 145.00, 72.00, 10, 'Arial'),

-- BOB Template Fields (template_id = 8)
(8, 'date', 155.00, 14.00, 12, 'Courier New'),
(8, 'name', 30.00, 28.00, 13, 'Arial'),
(8, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(8, 'amount', 150.00, 45.00, 14, 'Consolas'),
(8, 'signature', 145.00, 72.00, 10, 'Arial'),

-- Canara Template Fields (template_id = 9)
(9, 'date', 155.00, 14.00, 12, 'Courier New'),
(9, 'name', 30.00, 28.00, 13, 'Arial'),
(9, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(9, 'amount', 150.00, 45.00, 14, 'Consolas'),
(9, 'signature', 145.00, 72.00, 10, 'Arial'),

-- IndusInd Template Fields (template_id = 11)
(11, 'date', 155.00, 14.00, 12, 'Courier New'),
(11, 'name', 30.00, 28.00, 13, 'Arial'),
(11, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(11, 'amount', 150.00, 45.00, 14, 'Consolas'),
(11, 'signature', 145.00, 72.00, 10, 'Arial');
