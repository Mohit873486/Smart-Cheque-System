-- Smart Cheque System user authentication schema
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    phone VARCHAR(32),
    company VARCHAR(120),
    address VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin','Manager','Operator','Auditor') NOT NULL DEFAULT 'Operator',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sample test users
INSERT INTO users (username, name, email, password, role)
VALUES
('admin', 'System Administrator', 'admin@smartcheque.local', 'admin123', 'Admin'),
('manager', 'Jane Manager', 'manager@smartcheque.local', 'manager123', 'Manager'),
('operator', 'Sam Operator', 'operator@smartcheque.local', 'operator123', 'Operator'),
('auditor', 'Alex Auditor', 'auditor@smartcheque.local', 'auditor123', 'Auditor');
