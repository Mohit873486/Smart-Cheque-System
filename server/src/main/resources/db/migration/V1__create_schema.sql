-- ============================================================
-- V1__create_schema.sql
-- ChequePro initial schema — creates all core tables
-- Flyway baseline migration
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. USERS / AUTH / RBAC ----------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    email           VARCHAR(128) NOT NULL UNIQUE,
    phone           VARCHAR(32),
    company         VARCHAR(120),
    address         VARCHAR(255),
    gst_number      VARCHAR(50),
    password        VARCHAR(255) NOT NULL,
    role            ENUM('Admin','User','Manager','Operator','Auditor') NOT NULL DEFAULT 'User',
    status          ENUM('Active','Disabled','Locked') NOT NULL DEFAULT 'Active',
    login_attempts  INT     NOT NULL DEFAULT 0,
    account_locked  BOOLEAN NOT NULL DEFAULT FALSE,
    locked_at       TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_identity (username, email),
    INDEX idx_users_role     (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_otps (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT    NOT NULL,
    otp_hash   VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_otps_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_password_reset_active (user_id, used_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(40)  NOT NULL UNIQUE,
    description    VARCHAR(255),
    is_system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BANK ACCOUNTS & TEMPLATES ----------------------------------

CREATE TABLE IF NOT EXISTS bank_account (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name            VARCHAR(100) NOT NULL,
    account_number       VARCHAR(35)  NOT NULL UNIQUE,
    account_holder_name  VARCHAR(150) NOT NULL,
    ifsc                 VARCHAR(11)  NOT NULL,
    branch               VARCHAR(100),
    signature_path       VARCHAR(255),
    template_id          BIGINT DEFAULT 1,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bank_templates (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    bank_name   VARCHAR(100) NOT NULL,
    bank_code   VARCHAR(20)  NOT NULL,
    cheque_size VARCHAR(50)  DEFAULT '8.5x3.66in',
    micr        BOOLEAN      DEFAULT TRUE,
    logo_path   VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bank_code (bank_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cheque_template (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id       BIGINT       NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    width         DOUBLE NOT NULL DEFAULT 203.20,
    height        DOUBLE NOT NULL DEFAULT 92.00,
    config_json   LONGTEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cheque_template_bank (bank_id, template_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS template_field (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT       NOT NULL,
    field_name  VARCHAR(50)  NOT NULL,
    x_position  DOUBLE NOT NULL,
    y_position  DOUBLE NOT NULL,
    font_size   INT    NOT NULL DEFAULT 12,
    font_family VARCHAR(50)  NOT NULL DEFAULT 'Arial',
    CONSTRAINT fk_template_field_template
        FOREIGN KEY (template_id) REFERENCES cheque_template(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_template_field_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounts (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    account_number      VARCHAR(30)  NOT NULL UNIQUE,
    account_holder_name VARCHAR(150) NOT NULL,
    bank_name           VARCHAR(100) NOT NULL,
    ifsc_code           VARCHAR(20)  NOT NULL,
    balance             DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. CHEQUES ----------------------------------------------------

CREATE TABLE IF NOT EXISTS cheques (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    cheque_no         VARCHAR(50)  NOT NULL UNIQUE,
    cheque_type       ENUM('ACCOUNT_PAYEE','SELF','BEARER') NOT NULL DEFAULT 'ACCOUNT_PAYEE',
    payee_name        VARCHAR(150) NOT NULL,
    amount            DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    amount_words      VARCHAR(600),
    bank_id           INT,
    account_id        INT NOT NULL DEFAULT 1,
    bank_name         VARCHAR(100) NOT NULL DEFAULT 'State Bank of India',
    issue_date        DATE NOT NULL,
    status            ENUM('Draft','Pending','Approved','Rejected','Printed','Cancelled','Deposited','Cleared','Bounced') NOT NULL DEFAULT 'Pending',
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    last_printer      VARCHAR(150),
    last_print_result VARCHAR(50),
    printed_at        TIMESTAMP NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cheque_status  (status),
    INDEX idx_cheque_date    (issue_date),
    INDEX idx_cheque_account (account_id),
    INDEX idx_cheque_active  (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. INVOICES ---------------------------------------------------

CREATE TABLE IF NOT EXISTS invoices (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no  VARCHAR(30)   NOT NULL UNIQUE,
    client_name VARCHAR(150)  NOT NULL,
    amount      DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    issue_date  DATE NOT NULL,
    due_date    DATE NOT NULL,
    status      ENUM('Unpaid','Paid','Partial','Cancelled') NOT NULL DEFAULT 'Unpaid',
    notes       TEXT,
    paid_at     TIMESTAMP NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invoice_status (status),
    INDEX idx_invoice_due    (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. AUDIT LOG & NOTIFICATIONS ----------------------------------

CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NULL,
    table_name VARCHAR(50),
    record_id  INT,
    action     ENUM('LOGIN','LOGOUT','INSERT','UPDATE','DELETE','PRINT','APPROVE','REJECT',
                    'RESET_PASSWORD','LOCK','UNLOCK','CLEARED','DEPOSITED','BOUNCED') NOT NULL,
    details    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_log_created (created_at),
    INDEX idx_audit_log_user    (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NULL,
    title      VARCHAR(160) NOT NULL,
    message    TEXT         NOT NULL,
    type       ENUM('INFO','APPROVAL','REMINDER','AUDIT','SYSTEM') NOT NULL DEFAULT 'INFO',
    status     ENUM('Unread','Read') NOT NULL DEFAULT 'Unread',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at    TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reminders (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NULL,
    table_name VARCHAR(50),
    record_id  INT,
    title      VARCHAR(160) NOT NULL,
    remind_at  TIMESTAMP NOT NULL,
    status     ENUM('Pending','Sent','Cancelled') NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminders_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reminders_due (status, remind_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. SETTINGS ---------------------------------------------------

CREATE TABLE IF NOT EXISTS settings (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    app_name       VARCHAR(100) NOT NULL DEFAULT 'ChequePro',
    currency       VARCHAR(50)  DEFAULT 'INR',
    date_format    VARCHAR(50)  DEFAULT 'dd-MM-yyyy',
    language       VARCHAR(50)  DEFAULT 'English',
    cheque_prefix  VARCHAR(20)  DEFAULT 'CHQ',
    default_bank   VARCHAR(100) DEFAULT NULL,
    auto_print     BOOLEAN DEFAULT FALSE,
    amount_confirm BOOLEAN DEFAULT TRUE,
    invoice_prefix VARCHAR(20)  DEFAULT 'INV',
    payment_terms  VARCHAR(50)  DEFAULT 'Net 30',
    auto_gst       BOOLEAN DEFAULT TRUE,
    theme          VARCHAR(20)  DEFAULT 'light'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
