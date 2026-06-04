-- ChequePro Database Schema v2.1
-- Fresh install:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS chequeprint_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chequeprint_db;

-- USERS / AUTH / RBAC
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    phone VARCHAR(32),
    company VARCHAR(120),
    address VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin','User','Manager','Operator','Auditor') NOT NULL DEFAULT 'User',
    status ENUM('Active','Disabled','Locked') NOT NULL DEFAULT 'Active',
    login_attempts INT NOT NULL DEFAULT 0,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    locked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_identity (username, email),
    INDEX idx_users_role (role)
);

CREATE TABLE IF NOT EXISTS password_reset_otps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_otps_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_password_reset_active (user_id, used_at, expires_at)
);

-- BANK TEMPLATES
CREATE TABLE IF NOT EXISTS bank_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    cheque_size VARCHAR(50) DEFAULT '8.5x3.66in',
    micr BOOLEAN DEFAULT TRUE,
    logo_path VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bank_code (bank_code)
);

-- CHEQUES
CREATE TABLE IF NOT EXISTS cheques (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cheque_no VARCHAR(30) NOT NULL UNIQUE,
    payee_name VARCHAR(150) NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    amount_words VARCHAR(600),
    bank_id INT,
    issue_date DATE NOT NULL,
    status ENUM('Draft','Pending','Approved','Rejected','Printed','Cancelled') NOT NULL DEFAULT 'Draft',
    printed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cheques_bank
        FOREIGN KEY (bank_id) REFERENCES bank_templates(id)
        ON DELETE SET NULL,
    INDEX idx_cheque_status (status),
    INDEX idx_cheque_date (issue_date)
);

-- INVOICES
CREATE TABLE IF NOT EXISTS invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(30) NOT NULL UNIQUE,
    client_name VARCHAR(150) NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('Unpaid','Paid','Partial','Cancelled') NOT NULL DEFAULT 'Unpaid',
    notes TEXT,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invoice_status (status),
    INDEX idx_invoice_due (due_date)
);

-- AUDIT LOG
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    table_name VARCHAR(50),
    record_id INT,
    action ENUM('LOGIN','LOGOUT','INSERT','UPDATE','DELETE','PRINT','APPROVE','RESET_PASSWORD','LOCK') NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL,
    INDEX idx_audit_log_created (created_at),
    INDEX idx_audit_log_user (user_id)
);

-- SETTINGS
CREATE TABLE IF NOT EXISTS settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(100) NOT NULL DEFAULT 'ChequePro',
    currency VARCHAR(50) DEFAULT 'INR',
    date_format VARCHAR(50) DEFAULT 'dd-MM-yyyy',
    language VARCHAR(50) DEFAULT 'English',
    cheque_prefix VARCHAR(20) DEFAULT 'CHQ',
    invoice_prefix VARCHAR(20) DEFAULT 'INV',
    theme VARCHAR(20) DEFAULT 'light'
);

-- DEFAULT USERS
-- Passwords:
--   admin/admin123, user/operator123, manager/manager123, operator/operator123, auditor/auditor123
-- Stored with BCrypt. Do not replace these with plain text in production.
INSERT INTO users (username, name, email, password, role)
VALUES
('admin', 'System Administrator', 'admin@smartcheque.local', '$2a$12$2WjvDQuov1Ip4u3lLDJp8e0AL63OpXKoaure2qdPE03mppQGdMlzy', 'Admin'),
('user', 'Finance User', 'user@smartcheque.local', '$2a$12$Z6i6s3tfmBzSfNuWF3TMnuM.XwcU.F/Db14AqeKr/Uvm7il4wFMvu', 'User'),
('manager', 'Jane Manager', 'manager@smartcheque.local', '$2a$12$NESffFiz1n53l/zxz7SEC.EtHK8EVFqaJMmNXtYaLMDLxJYDsvcXW', 'Manager'),
('operator', 'Operator One', 'operator@smartcheque.local', '$2a$12$Z6i6s3tfmBzSfNuWF3TMnuM.XwcU.F/Db14AqeKr/Uvm7il4wFMvu', 'Operator'),
('auditor', 'Audit Specialist', 'auditor@smartcheque.local', '$2a$12$KaIfxRet1c2C61EufUFF2On0krXBca20DDvy32CyU0K7Ko.LMjUHS', 'Auditor')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    role = VALUES(role);

-- BANK TEMPLATES
INSERT IGNORE INTO bank_templates (bank_name, bank_code, cheque_size, micr) VALUES
('State Bank of India', 'SBI', '8.5x3.66in', TRUE),
('HDFC Bank', 'HDFC', '8.5x3.66in', TRUE),
('ICICI Bank', 'ICICI', '8.5x3.66in', TRUE),
('Axis Bank', 'AXIS', '8.5x3.66in', TRUE),
('Punjab National Bank', 'PNB', '8.5x3.66in', TRUE),
('Bank of Baroda', 'BOB', '8.5x3.66in', TRUE),
('Kotak Mahindra Bank', 'KMB', '8.5x3.66in', TRUE),
('IndusInd Bank', 'IIB', '8.5x3.66in', TRUE);

-- SAMPLE CHEQUES
INSERT IGNORE INTO cheques (cheque_no, payee_name, amount, amount_words, bank_id, issue_date, status)
VALUES
('CHQ-2025-001', 'Raj Electricals', 25000.00, 'Rupees Twenty Five Thousand Only', 1, CURDATE(), 'Printed'),
('CHQ-2025-002', 'Sharma Traders', 12500.50, 'Rupees Twelve Thousand Five Hundred Only', 2, CURDATE(), 'Pending'),
('CHQ-2025-003', 'City Suppliers', 8750.00, 'Rupees Eight Thousand Seven Hundred Fifty Only', 3, CURDATE(), 'Draft'),
('CHQ-2025-004', 'Global Tech Pvt Ltd', 55000.00, 'Rupees Fifty Five Thousand Only', 4, CURDATE(), 'Printed');

-- SAMPLE INVOICES
INSERT IGNORE INTO invoices (invoice_no, client_name, amount, issue_date, due_date, status, notes)
VALUES
('INV-2025-001', 'Mehta & Sons', 45000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'Unpaid', 'Net 30 terms'),
('INV-2025-002', 'TechStart Solutions', 18500.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'Paid', 'Advance payment received'),
('INV-2025-003', 'Sunrise Enterprises', 32000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'Partial', '50% paid on delivery');

INSERT INTO settings (id, app_name)
VALUES (1, 'ChequePro')
ON DUPLICATE KEY UPDATE app_name = VALUES(app_name);

-- Quick checks:
-- SELECT id, username, email, role, login_attempts, account_locked FROM users;
-- SHOW TABLES;
