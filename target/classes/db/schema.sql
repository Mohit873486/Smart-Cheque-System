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

CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255)
);

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

-- ACCOUNTS
CREATE TABLE IF NOT EXISTS accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_holder_name VARCHAR(150) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- CHEQUES
CREATE TABLE IF NOT EXISTS cheques (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cheque_no VARCHAR(30) NOT NULL UNIQUE,
    payee_name VARCHAR(150) NOT NULL,
    amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    amount_words VARCHAR(600),
    bank_id INT,
    account_id INT NOT NULL,
    issue_date DATE NOT NULL,
    status ENUM('Draft','Pending','Approved','Rejected','Printed','Cancelled','Deposited','Cleared','Bounced') NOT NULL DEFAULT 'Draft',
    printed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cheques_bank
        FOREIGN KEY (bank_id) REFERENCES bank_templates(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_cheques_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE RESTRICT,
    INDEX idx_cheque_status (status),
    INDEX idx_cheque_date (issue_date),
    INDEX idx_cheque_account (account_id)
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
    action ENUM('LOGIN','LOGOUT','INSERT','UPDATE','DELETE','PRINT','APPROVE','REJECT','RESET_PASSWORD','LOCK','UNLOCK') NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL,
    INDEX idx_audit_log_created (created_at),
    INDEX idx_audit_log_user (user_id)
);

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
);

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
);

-- SETTINGS
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
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.name = 'Admin';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'VIEW_DASHBOARD','VIEW_CHEQUES','APPROVE_CHEQUE','REJECT_CHEQUE','PRINT_CHEQUE',
    'VIEW_INVOICES','VIEW_REPORTS','VIEW_SUPPORT','VIEW_PROFILE','UPDATE_PROFILE'
)
WHERE r.name = 'Manager';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'VIEW_DASHBOARD','VIEW_CHEQUES','CREATE_CHEQUE','UPDATE_CHEQUE','SUBMIT_CHEQUE',
    'VIEW_SUPPORT','VIEW_PROFILE','UPDATE_PROFILE'
)
WHERE r.name = 'Operator';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'VIEW_DASHBOARD','VIEW_CHEQUES','VIEW_INVOICES','VIEW_REPORTS',
    'VIEW_BANK_TEMPLATES','VIEW_SUPPORT','VIEW_PROFILE','VIEW_AUDIT_LOG'
)
WHERE r.name = 'Auditor';

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

-- SAMPLE ACCOUNTS
INSERT IGNORE INTO accounts (id, account_number, account_holder_name, bank_name, branch_name, ifsc_code, balance)
VALUES
(1, '123456789012', 'Acme Corp Operating Account', 'State Bank of India', 'Mumbai Main Branch', 'SBIN0000123', 5000000.00),
(2, '987654321098', 'Acme Corp Payroll Account', 'HDFC Bank', 'Delhi Connaught Place Branch', 'HDFC0000456', 2500000.00);

-- SAMPLE CHEQUES
INSERT IGNORE INTO cheques (cheque_no, payee_name, amount, amount_words, bank_id, account_id, issue_date, status)
VALUES
('CHQ-2025-001', 'Raj Electricals', 25000.00, 'Rupees Twenty Five Thousand Only', 1, 1, CURDATE(), 'Printed'),
('CHQ-2025-002', 'Sharma Traders', 12500.50, 'Rupees Twelve Thousand Five Hundred Only', 2, 1, CURDATE(), 'Pending'),
('CHQ-2025-003', 'City Suppliers', 8750.00, 'Rupees Eight Thousand Seven Hundred Fifty Only', 3, 2, CURDATE(), 'Draft'),
('CHQ-2025-004', 'Global Tech Pvt Ltd', 55000.00, 'Rupees Fifty Five Thousand Only', 4, 2, CURDATE(), 'Printed');

-- SAMPLE INVOICES
INSERT IGNORE INTO invoices (invoice_no, client_name, amount, issue_date, due_date, status, notes)
VALUES
('INV-2025-001', 'Mehta & Sons', 45000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'Unpaid', 'Net 30 terms'),
('INV-2025-002', 'TechStart Solutions', 18500.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'Paid', 'Advance payment received'),
('INV-2025-003', 'Sunrise Enterprises', 32000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'Partial', '50% paid on delivery');

INSERT INTO settings (id, app_name)
VALUES (1, 'ChequePro')
AS new_settings
ON DUPLICATE KEY UPDATE app_name = new_settings.app_name;

-- Quick checks:
-- SELECT id, username, email, role, login_attempts, account_locked FROM users;
-- SHOW TABLES;
