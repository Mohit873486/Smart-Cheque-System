-- ============================================================
-- ChequePro Complete Enterprise Database Schema v2.2
-- Supports RBAC, Audit Logging, Cheque Types & Template Designer
-- Fresh install: mysql -u root -p < cheque_printing_schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS chequeprint_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chequeprint_db;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. USERS / AUTH / RBAC
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    phone VARCHAR(32),
    company VARCHAR(120),
    address VARCHAR(255),
    gst_number VARCHAR(50),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. BANK ACCOUNTS & TEMPLATES
CREATE TABLE IF NOT EXISTS bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(35) NOT NULL UNIQUE,
    account_holder_name VARCHAR(150) NOT NULL,
    ifsc VARCHAR(11) NOT NULL,
    branch VARCHAR(100),
    signature_path VARCHAR(255),
    template_id BIGINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    cheque_size VARCHAR(50) DEFAULT '8.5x3.66in',
    micr BOOLEAN DEFAULT TRUE,
    logo_path VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bank_code (bank_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cheque_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id BIGINT NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    width DOUBLE NOT NULL DEFAULT 203.20,
    height DOUBLE NOT NULL DEFAULT 92.00,
    config_json LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cheque_template_bank (bank_id, template_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS template_field (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    x_position DOUBLE NOT NULL,
    y_position DOUBLE NOT NULL,
    font_size INT NOT NULL DEFAULT 12,
    font_family VARCHAR(50) NOT NULL DEFAULT 'Arial',
    CONSTRAINT fk_template_field_template
        FOREIGN KEY (template_id) REFERENCES cheque_template(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_template_field_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_holder_name VARCHAR(150) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. UNIFIED CHEQUES TABLE
CREATE TABLE IF NOT EXISTS cheques (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cheque_no VARCHAR(50) NOT NULL UNIQUE,
    cheque_type ENUM('ACCOUNT_PAYEE', 'SELF', 'BEARER') NOT NULL DEFAULT 'ACCOUNT_PAYEE',
    payee_name VARCHAR(150) NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    amount_words VARCHAR(600),
    bank_id INT,
    account_id INT NOT NULL DEFAULT 1,
    bank_name VARCHAR(100) NOT NULL DEFAULT 'State Bank of India',
    issue_date DATE NOT NULL,
    status ENUM('Draft','Pending','Approved','Rejected','Printed','Cancelled','Deposited','Cleared','Bounced') NOT NULL DEFAULT 'Pending',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_printer VARCHAR(150),
    last_print_result VARCHAR(50),
    printed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cheque_status (status),
    INDEX idx_cheque_date (issue_date),
    INDEX idx_cheque_account (account_id),
    INDEX idx_cheque_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. INVOICES
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. AUDIT LOG & NOTIFICATIONS
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    table_name VARCHAR(50),
    record_id INT,
    action ENUM('LOGIN','LOGOUT','INSERT','UPDATE','DELETE','PRINT','APPROVE','REJECT','RESET_PASSWORD','LOCK','UNLOCK','CLEARED','DEPOSITED','BOUNCED') NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL,
    INDEX idx_audit_log_created (created_at),
    INDEX idx_audit_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO','APPROVAL','REMINDER','AUDIT','SYSTEM') NOT NULL DEFAULT 'INFO',
    status ENUM('Unread','Read') NOT NULL DEFAULT 'Unread',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_notifications_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    table_name VARCHAR(50),
    record_id INT,
    title VARCHAR(160) NOT NULL,
    remind_at TIMESTAMP NOT NULL,
    status ENUM('Pending','Sent','Cancelled') NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminders_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_reminders_due (status, remind_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. SETTINGS
CREATE TABLE IF NOT EXISTS settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(100) NOT NULL DEFAULT 'ChequePro',
    currency VARCHAR(50) DEFAULT 'INR',
    date_format VARCHAR(50) DEFAULT 'dd-MM-yyyy',
    language VARCHAR(50) DEFAULT 'English',
    cheque_prefix VARCHAR(20) DEFAULT 'CHQ',
    default_bank VARCHAR(100) DEFAULT NULL,
    auto_print BOOLEAN DEFAULT FALSE,
    amount_confirm BOOLEAN DEFAULT TRUE,
    invoice_prefix VARCHAR(20) DEFAULT 'INV',
    payment_terms VARCHAR(50) DEFAULT 'Net 30',
    auto_gst BOOLEAN DEFAULT TRUE,
    theme VARCHAR(20) DEFAULT 'light'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- INITIAL SEED DATA INSERTION
-- ============================================================

-- Default Users
INSERT INTO users (username, name, email, password, role)
VALUES
('admin', 'System Administrator', 'admin@smartcheque.local', '$2a$12$2WjvDQuov1Ip4u3lLDJp8e0AL63OpXKoaure2qdPE03mppQGdMlzy', 'Admin'),
('user', 'Finance User', 'user@smartcheque.local', '$2a$12$Z6i6s3tfmBzSfNuWF3TMnuM.XwcU.F/Db14AqeKr/Uvm7il4wFMvu', 'User'),
('manager', 'Jane Manager', 'manager@smartcheque.local', '$2a$12$NESffFiz1n53l/zxz7SEC.EtHK8EVFqaJMmNXtYaLMDLxJYDsvcXW', 'Manager'),
('operator', 'Operator One', 'operator@smartcheque.local', '$2a$12$Z6i6s3tfmBzSfNuWF3TMnuM.XwcU.F/Db14AqeKr/Uvm7il4wFMvu', 'Operator'),
('auditor', 'Audit Specialist', 'auditor@smartcheque.local', '$2a$12$KaIfxRet1c2C61EufUFF2On0krXBca20DDvy32CyU0K7Ko.LMjUHS', 'Auditor')
AS new_users
ON DUPLICATE KEY UPDATE
    name = new_users.name,
    role = new_users.role;

INSERT INTO roles (name, description) VALUES
('Admin', 'Full system administrator'),
('Manager', 'Approves and prints cheques'),
('Operator', 'Creates and submits cheques'),
('Auditor', 'Read-only compliance reviewer')
AS new_roles
ON DUPLICATE KEY UPDATE description = new_roles.description;

INSERT INTO permissions (code, description) VALUES
('VIEW_DASHBOARD', 'Open role dashboard'),
('VIEW_CHEQUES', 'View cheque records'),
('CREATE_CHEQUE', 'Create and submit cheques'),
('UPDATE_CHEQUE', 'Edit cheque drafts or submitted records'),
('DELETE_CHEQUE', 'Delete cheque records'),
('SUBMIT_CHEQUE', 'Submit cheque for approval'),
('APPROVE_CHEQUE', 'Approve pending cheques'),
('REJECT_CHEQUE', 'Reject pending cheques'),
('PRINT_CHEQUE', 'Print approved cheques'),
('VIEW_INVOICES', 'View invoice records'),
('VIEW_REPORTS', 'View finance and audit reports'),
('VIEW_BANK_TEMPLATES', 'View bank templates'),
('ACCESS_AI_ASSISTANT', 'Use AI assistant tools'),
('VIEW_SUPPORT', 'Open support page'),
('VIEW_PROFILE', 'Open own profile'),
('UPDATE_PROFILE', 'Update own profile'),
('MANAGE_SETTINGS', 'Manage system settings'),
('MANAGE_USERS', 'Manage users and roles'),
('VIEW_AUDIT_LOG', 'View audit compliance logs')
AS new_perms
ON DUPLICATE KEY UPDATE description = new_perms.description;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.name = 'Admin';

-- Bank Accounts & Templates
INSERT INTO bank_account (id, bank_name, account_number, account_holder_name, ifsc, branch) VALUES
(1, 'State Bank of India', '123456789012', 'Acme Corp Operating Account', 'SBIN0000123', 'Mumbai Main Branch'),
(2, 'HDFC Bank', '987654321098', 'Acme Corp Payroll Account', 'HDFC0000456', 'Delhi Connaught Place Branch'),
(3, 'ICICI Bank', '556677889900', 'Acme Corp Reserve Account', 'ICIC0000789', 'Bandra West Branch'),
(4, 'Axis Bank', '112233445566', 'Acme Corp Vendor Account', 'UTIB0000112', 'MG Road Branch'),
(6, 'Kotak Mahindra Bank', '667788990011', 'Acme Corp Tax Account', 'KKBK0000667', 'Nariman Point Branch'),
(7, 'Punjab National Bank', '778899001122', 'Acme Corp Treasury Account', 'PUNB0000778', 'Connaught Place Branch'),
(8, 'Bank of Baroda', '889900112233', 'Acme Corp Escrow Account', 'BARB0000889', 'Alkapuri Branch'),
(9, 'Canara Bank', '990011223344', 'Acme Corp Dividend Account', 'CNRB0000990', 'Town Hall Branch'),
(11, 'IndusInd Bank', '110022334455', 'Acme Corp Operations Account', 'INDB0000111', 'Cyber City Branch')
ON DUPLICATE KEY UPDATE bank_name = VALUES(bank_name);

INSERT IGNORE INTO bank_templates (id, bank_name, bank_code, cheque_size, micr) VALUES
(1, 'State Bank of India', 'SBI', '8.5x3.66in', TRUE),
(2, 'HDFC Bank', 'HDFC', '8.5x3.66in', TRUE),
(3, 'ICICI Bank', 'ICICI', '8.5x3.66in', TRUE),
(4, 'Axis Bank', 'AXIS', '8.5x3.66in', TRUE),
(6, 'Kotak Mahindra Bank', 'KMB', '8.5x3.66in', TRUE),
(7, 'Punjab National Bank', 'PNB', '8.5x3.66in', TRUE),
(8, 'Bank of Baroda', 'BOB', '8.5x3.66in', TRUE),
(9, 'Canara Bank', 'CNB', '8.5x3.66in', TRUE),
(11, 'IndusInd Bank', 'IIB', '8.5x3.66in', TRUE);

INSERT INTO cheque_template (id, bank_id, template_name, width, height) VALUES
(1, 1, 'SBI CTS-2010 Standard Cheque', 203.20, 92.00),
(2, 2, 'HDFC Corporate Premium Cheque', 203.20, 92.00),
(3, 3, 'ICICI Commercial Cheque', 203.20, 92.00),
(4, 4, 'Axis Bank Business Cheque', 203.20, 92.00),
(6, 6, 'Kotak Corporate Cheque Template', 203.20, 92.00),
(7, 7, 'PNB Standard Cheque Template', 203.20, 92.00),
(8, 8, 'BOB Business Cheque Template', 203.20, 92.00),
(9, 9, 'Canara Bank Cheque Template', 203.20, 92.00),
(11, 11, 'IndusInd Commercial Template', 203.20, 92.00)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

INSERT IGNORE INTO template_field (template_id, field_name, x_position, y_position, font_size, font_family) VALUES
(1, 'date', 155.00, 14.00, 12, 'Courier New'),
(1, 'name', 30.00, 28.00, 13, 'Arial'),
(1, 'amount_words', 35.00, 42.00, 11, 'Arial'),
(1, 'amount', 150.00, 45.00, 14, 'Consolas'),
(1, 'signature', 145.00, 72.00, 10, 'Arial');

INSERT IGNORE INTO accounts (id, account_number, account_holder_name, bank_name, ifsc_code, balance) VALUES
(1, '123456789012', 'Acme Corp Operating Account', 'State Bank of India', 'SBIN0000123', 5000000.00),
(2, '987654321098', 'Acme Corp Payroll Account', 'HDFC Bank', 'HDFC0000456', 2500000.00);

-- Sample Cheques with Cheque Types
INSERT INTO cheques (cheque_no, cheque_type, payee_name, amount, amount_words, bank_id, account_id, bank_name, issue_date, status, is_active) VALUES
('CHQ-1785477171', 'ACCOUNT_PAYEE', 'rohit2', 45.00, 'Rupees Forty Five Only', 8, 1, 'Bank of Baroda', '2026-07-31', 'Pending', TRUE),
('CHQ-1785478136', 'ACCOUNT_PAYEE', 'mohit', 7.00, 'Rupees Seven Only', 1, 1, 'State Bank of India', '2026-07-31', 'Printed', TRUE),
('CHQ-1785478893', 'ACCOUNT_PAYEE', 'rakesh', 5.00, 'Rupees Five Only', 2, 1, 'HDFC Bank', '2026-07-31', 'Pending', TRUE),
('CHQ-1785481850', 'ACCOUNT_PAYEE', 'killed 2', 50.00, 'Rupees Fifty Only', 8, 1, 'Bank of Baroda', '2026-07-31', 'Cleared', TRUE),
('CHQ-1785481999', 'SELF', 'maynk', 70.00, 'Rupees Seventy Only', 1, 1, 'State Bank of India', '2026-07-31', 'Pending', TRUE),
('CHQ-1785489835', 'ACCOUNT_PAYEE', 'Kanu', 99.00, 'Rupees Ninety Nine Only', 6, 1, 'Kotak Mahindra Bank', '2026-07-31', 'Pending', TRUE),
('CHQ-1785498530', 'BEARER', 'radhe', 5500.00, 'Rupees Five Thousand Five Hundred Only', 11, 1, 'IndusInd Bank', '2026-07-31', 'Printed', TRUE)
ON DUPLICATE KEY UPDATE payee_name = VALUES(payee_name), bank_name = VALUES(bank_name);

INSERT INTO settings (id, app_name) VALUES (1, 'ChequePro')
ON DUPLICATE KEY UPDATE app_name = VALUES(app_name);
