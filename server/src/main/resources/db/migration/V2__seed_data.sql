-- ============================================================
-- V2__seed_data.sql
-- ChequePro initial seed / reference data
-- Safe to re-run: uses ON DUPLICATE KEY UPDATE / INSERT IGNORE
-- ============================================================

-- Default users (passwords are BCrypt hashes)
-- admin / admin123 | user / user123 | manager / manager123
INSERT INTO users (username, name, email, password, role, status)
VALUES
    ('admin',    'System Administrator', 'admin@smartcheque.local',    '$2a$12$2WjvDQuov1Ip4u3lLDJp8e0AL63OpXKoaure2qdPE03mppQGdMlzy', 'Admin',    'Active'),
    ('user',     'Finance User',         'user@smartcheque.local',     '$2a$12$Z6i6s3tfmBzSfNuWF3TMnuM.XwcU.F/Db14AqeKr/Uvm7il4wFMvu', 'User',     'Active'),
    ('manager',  'Jane Manager',         'manager@smartcheque.local',  '$2a$12$NESffFiz1n53l/zxz7SEC.EtHK8EVFqaJMmNXtYaLMDLxJYDsvcXW', 'Manager',  'Active'),
    ('operator', 'Operator One',         'operator@smartcheque.local', '$2a$12$Z6i6s3tfmBzSfNuWF3TMnuM.XwcU.F/Db14AqeKr/Uvm7il4wFMvu', 'Operator', 'Active'),
    ('auditor',  'Audit Specialist',     'auditor@smartcheque.local',  '$2a$12$KaIfxRet1c2C61EufUFF2On0krXBca20DDvy32CyU0K7Ko.LMjUHS',  'Auditor',  'Active')
AS new_users
ON DUPLICATE KEY UPDATE
    name = new_users.name,
    role = new_users.role,
    status = new_users.status;

-- Roles
INSERT INTO roles (name, description) VALUES
    ('Admin',    'Full system administrator'),
    ('Manager',  'Approves and prints cheques'),
    ('Operator', 'Creates and submits cheques'),
    ('Auditor',  'Read-only compliance reviewer')
AS new_roles
ON DUPLICATE KEY UPDATE description = new_roles.description;

-- Permissions
INSERT INTO permissions (code, description) VALUES
    ('VIEW_DASHBOARD',      'Open role dashboard'),
    ('VIEW_CHEQUES',        'View cheque records'),
    ('CREATE_CHEQUE',       'Create and submit cheques'),
    ('UPDATE_CHEQUE',       'Edit cheque drafts or submitted records'),
    ('DELETE_CHEQUE',       'Delete cheque records'),
    ('SUBMIT_CHEQUE',       'Submit cheque for approval'),
    ('APPROVE_CHEQUE',      'Approve pending cheques'),
    ('REJECT_CHEQUE',       'Reject pending cheques'),
    ('PRINT_CHEQUE',        'Print approved cheques'),
    ('VIEW_INVOICES',       'View invoice records'),
    ('VIEW_REPORTS',        'View finance and audit reports'),
    ('VIEW_BANK_TEMPLATES', 'View bank templates'),
    ('ACCESS_AI_ASSISTANT', 'Use AI assistant tools'),
    ('VIEW_SUPPORT',        'Open support page'),
    ('VIEW_PROFILE',        'Open own profile'),
    ('UPDATE_PROFILE',      'Update own profile'),
    ('MANAGE_SETTINGS',     'Manage system settings'),
    ('MANAGE_USERS',        'Manage users and roles'),
    ('VIEW_AUDIT_LOG',      'View audit compliance logs')
AS new_perms
ON DUPLICATE KEY UPDATE description = new_perms.description;

-- Grant all permissions to Admin role
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.name = 'Admin';

-- Bank accounts
INSERT INTO bank_account (id, bank_name, account_number, account_holder_name, ifsc, branch) VALUES
    (1,  'State Bank of India',    '123456789012', 'Acme Corp Operating Account',  'SBIN0000123', 'Mumbai Main Branch'),
    (2,  'HDFC Bank',              '987654321098', 'Acme Corp Payroll Account',    'HDFC0000456', 'Delhi Connaught Place Branch'),
    (3,  'ICICI Bank',             '556677889900', 'Acme Corp Reserve Account',    'ICIC0000789', 'Bandra West Branch'),
    (4,  'Axis Bank',              '112233445566', 'Acme Corp Vendor Account',     'UTIB0000112', 'MG Road Branch'),
    (6,  'Kotak Mahindra Bank',    '667788990011', 'Acme Corp Tax Account',        'KKBK0000667', 'Nariman Point Branch'),
    (7,  'Punjab National Bank',   '778899001122', 'Acme Corp Treasury Account',   'PUNB0000778', 'Connaught Place Branch'),
    (8,  'Bank of Baroda',         '889900112233', 'Acme Corp Escrow Account',     'BARB0000889', 'Alkapuri Branch'),
    (9,  'Canara Bank',            '990011223344', 'Acme Corp Dividend Account',   'CNRB0000990', 'Town Hall Branch'),
    (11, 'IndusInd Bank',          '110022334455', 'Acme Corp Operations Account', 'INDB0000111', 'Cyber City Branch')
ON DUPLICATE KEY UPDATE bank_name = VALUES(bank_name);

-- Bank templates
INSERT IGNORE INTO bank_templates (id, bank_name, bank_code, cheque_size, micr) VALUES
    (1,  'State Bank of India',  'SBI',  '8.5x3.66in', TRUE),
    (2,  'HDFC Bank',            'HDFC', '8.5x3.66in', TRUE),
    (3,  'ICICI Bank',           'ICICI','8.5x3.66in', TRUE),
    (4,  'Axis Bank',            'AXIS', '8.5x3.66in', TRUE),
    (6,  'Kotak Mahindra Bank',  'KMB',  '8.5x3.66in', TRUE),
    (7,  'Punjab National Bank', 'PNB',  '8.5x3.66in', TRUE),
    (8,  'Bank of Baroda',       'BOB',  '8.5x3.66in', TRUE),
    (9,  'Canara Bank',          'CNB',  '8.5x3.66in', TRUE),
    (11, 'IndusInd Bank',        'IIB',  '8.5x3.66in', TRUE);

-- Cheque templates
INSERT INTO cheque_template (id, bank_id, template_name, width, height) VALUES
    (1,  1,  'SBI CTS-2010 Standard Cheque',    203.20, 92.00),
    (2,  2,  'HDFC Corporate Premium Cheque',   203.20, 92.00),
    (3,  3,  'ICICI Commercial Cheque',          203.20, 92.00),
    (4,  4,  'Axis Bank Business Cheque',        203.20, 92.00),
    (6,  6,  'Kotak Corporate Cheque Template',  203.20, 92.00),
    (7,  7,  'PNB Standard Cheque Template',     203.20, 92.00),
    (8,  8,  'BOB Business Cheque Template',     203.20, 92.00),
    (9,  9,  'Canara Bank Cheque Template',      203.20, 92.00),
    (11, 11, 'IndusInd Commercial Template',     203.20, 92.00)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

-- SBI template fields
INSERT IGNORE INTO template_field (template_id, field_name, x_position, y_position, font_size, font_family) VALUES
    (1, 'date',         155.00, 14.00, 12, 'Courier New'),
    (1, 'name',          30.00, 28.00, 13, 'Arial'),
    (1, 'amount_words',  35.00, 42.00, 11, 'Arial'),
    (1, 'amount',       150.00, 45.00, 14, 'Consolas'),
    (1, 'signature',    145.00, 72.00, 10, 'Arial');

-- Default accounts
INSERT IGNORE INTO accounts (id, account_number, account_holder_name, bank_name, ifsc_code, balance) VALUES
    (1, '123456789012', 'Acme Corp Operating Account', 'State Bank of India', 'SBIN0000123', 5000000.00),
    (2, '987654321098', 'Acme Corp Payroll Account',   'HDFC Bank',           'HDFC0000456', 2500000.00);

-- Default app settings row
INSERT INTO settings (id, app_name) VALUES (1, 'ChequePro')
ON DUPLICATE KEY UPDATE app_name = VALUES(app_name);
