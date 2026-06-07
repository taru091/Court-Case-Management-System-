CREATE DATABASE IF NOT EXISTS ccms_db;
USE ccms_db;

DROP TABLE IF EXISTS lawyer_notes;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS hearings;
DROP TABLE IF EXISTS approval_requests;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS change_requests;
DROP TABLE IF EXISTS admin_notifications;
DROP TABLE IF EXISTS site_settings;
DROP TABLE IF EXISTS cases;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    mobile VARCHAR(15),
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin', 'Lawyer', 'Judge', 'Staff', 'Citizen') NOT NULL,
    occupation VARCHAR(50),
    bar_council_number VARCHAR(50),
    court_id VARCHAR(50),
    aadhaar_number VARCHAR(20),
    profile_photo_url VARCHAR(255),
    approval_status VARCHAR(20) NOT NULL DEFAULT 'Approved',
    availability_status VARCHAR(20) NOT NULL DEFAULT 'Available'
);

CREATE TABLE cases (
    id INT PRIMARY KEY AUTO_INCREMENT,
    case_name VARCHAR(150) NOT NULL,
    client VARCHAR(120) NOT NULL,
    lawyer VARCHAR(120) NOT NULL,
    lawyer_user_id INT NULL,
    judge VARCHAR(120) NOT NULL,
    judge_user_id INT NULL,
    status ENUM('Active', 'Pending', 'Closed') NOT NULL,
    court_details VARCHAR(160) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hearings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    case_id INT NOT NULL,
    date DATETIME NOT NULL,
    courtroom VARCHAR(100) NOT NULL,
    judge_user_id INT NULL,
    CONSTRAINT fk_hearing_case
        FOREIGN KEY (case_id) REFERENCES cases(id)
        ON DELETE CASCADE
);

CREATE TABLE documents (
    id INT PRIMARY KEY AUTO_INCREMENT,
    case_id INT NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_name VARCHAR(200) NULL,
    mime_type VARCHAR(120) NULL,
    uploaded_by_user_id INT NULL,
    uploaded_by_role VARCHAR(40) NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    is_official BOOLEAN NOT NULL DEFAULT TRUE,
    rejection_reason TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_case
        FOREIGN KEY (case_id) REFERENCES cases(id)
        ON DELETE CASCADE
);

CREATE TABLE lawyer_notes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    case_id INT NOT NULL,
    hearing_id INT NULL,
    lawyer_user_id INT NOT NULL,
    lawyer_name VARCHAR(120) NOT NULL,
    note_type VARCHAR(60) NOT NULL,
    content LONGTEXT NOT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lawyer_note_case
        FOREIGN KEY (case_id) REFERENCES cases(id)
        ON DELETE CASCADE
);

CREATE TABLE admin_notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(40) NOT NULL,
    related_request_id INT NULL,
    target_role VARCHAR(40) NULL,
    target_user_id INT NULL,
    target_user_name VARCHAR(120) NULL,
    created_by_user_id INT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE site_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_by_user_id INT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE change_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(40) NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    entity_id INT NULL,
    request_title VARCHAR(180) NOT NULL,
    request_payload LONGTEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_by_user_id INT NOT NULL,
    requested_by_name VARCHAR(120) NOT NULL,
    requested_by_role VARCHAR(40) NOT NULL,
    judge_reviewer_id INT NULL,
    judge_reviewer_name VARCHAR(120) NULL,
    admin_reviewer_id INT NULL,
    admin_reviewer_name VARCHAR(120) NULL,
    review_note TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE approval_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    request_type VARCHAR(80) NOT NULL,
    requested_by_role VARCHAR(40) NOT NULL,
    requested_by_user INT NOT NULL,
    requested_by_name VARCHAR(120) NOT NULL,
    approval_role VARCHAR(40) NOT NULL,
    target_entity_type VARCHAR(40) NOT NULL,
    target_entity_id INT NULL,
    action_type VARCHAR(20) NOT NULL,
    request_title VARCHAR(180) NOT NULL,
    request_payload LONGTEXT NULL,
    before_payload LONGTEXT NULL,
    after_payload LONGTEXT NULL,
    live_change_applied BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT NULL,
    reviewed_by_user_id INT NULL,
    reviewed_by_user_name VARCHAR(120) NULL,
    reviewed_by_role VARCHAR(40) NULL,
    review_note TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    user_name VARCHAR(120) NULL,
    role VARCHAR(40) NULL,
    action TEXT NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Passwords are SHA-256 hashes for:
-- admin, lawyer123, staff123, judge123, citizen123
INSERT INTO users (
    username,
    name,
    mobile,
    email,
    password,
    role,
    occupation,
    bar_council_number,
    court_id,
    aadhaar_number,
    profile_photo_url,
    approval_status,
    availability_status
) VALUES
('admin', 'Admin', '9999999991', 'admin@ccms.com', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'Admin', 'Admin', NULL, NULL, '111122223333', NULL, 'Approved', 'Available'),
('lawyer', 'Lawyer', '9999999992', 'lawyer@ccms.com', 'ac3226b60081e5f9f9f1f784838aca038eb7c2f7411cb90702b6c2bfe07a45a9', 'Lawyer', 'Lawyer', 'BCI-LAW-1001', NULL, '222233334444', NULL, 'Approved', 'Available'),
('staff', 'Staff', '9999999993', 'staff@ccms.com', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Staff', 'Staff', NULL, NULL, '333344445555', NULL, 'Approved', 'Available'),
('judge', 'Judge', '9999999994', 'judge@ccms.com', '94358d5abe1d055c1ced4403bb0e397edf8c905b33e03b34ad1b1d3adf2d9cf4', 'Judge', 'Judge', NULL, 'COURT-101', '444455556666', NULL, 'Approved', 'Available'),
('citizen', 'Citizen', '9999999995', 'citizen@ccms.com', '4b4b4c19fdc4b422ca5a52085c3ba8fd2087c62afb06dae791f8fb9c51c56b4b', 'Citizen', 'Citizen', NULL, NULL, '555566667777', NULL, 'Approved', 'Available');

INSERT INTO site_settings (setting_key, setting_value, updated_by_user_id) VALUES
('publicHomeTitle', 'Court Case Management System', NULL),
('publicHomeSummary', 'Public access to main judiciary information, updates, and guided case search.', NULL),
('publicHomeNotice', 'Detailed case access is available after login for citizens, lawyers, judges, admins, and staff.', NULL),
('dashboardNotice', 'Admin actions are saved immediately and remain under approval review until finalized.', NULL),
('aiReferenceNote', 'Add hearing notes, FIR extracts, timelines, or document text here for better answers.', NULL),
('aiBehaviorNote', 'Prefer concise, procedural, India-specific legal guidance and clearly separate facts, issues, and next steps.', NULL);

INSERT INTO cases (id, case_name, client, lawyer, lawyer_user_id, judge, judge_user_id, status, court_details, created_at) VALUES
(1, 'State vs Turner Holdings', 'Avery Turner', 'Lawyer', 2, 'Judge', 4, 'Active', 'District Court Hall A', '2026-04-10 10:30:00'),
(2, 'Riverside Property Appeal', 'Riverside Group', 'Lawyer', 2, 'Judge', 4, 'Pending', 'District Court Hall B', '2026-04-18 14:15:00'),
(3, 'Maya Foods Contract Review', 'Maya Foods Ltd.', 'Lawyer', 2, 'Judge', 4, 'Closed', 'District Court Hall C', '2026-03-28 12:00:00'),
(4, 'City Works Compliance Matter', 'City Works', 'Lawyer', 2, 'Judge', 4, 'Active', 'District Court Hall A', '2026-04-25 16:10:00'),
(5, 'Northwind Evidence Review', 'Northwind Services', 'Lawyer', 2, 'Judge', 4, 'Pending', 'District Court Hall E', '2026-04-30 09:20:00'),
(6, 'Horizon Estate Settlement', 'Laura Benton', 'Lawyer', 2, 'Judge', 4, 'Active', 'District Court Hall F', '2026-05-01 08:50:00');

INSERT INTO hearings (id, case_id, date, courtroom, judge_user_id) VALUES
(1, 1, '2026-05-12 11:00:00', 'Courtroom A', 4),
(2, 1, '2026-05-28 10:00:00', 'Courtroom B', 4),
(3, 2, '2026-05-20 09:30:00', 'Courtroom C', 4),
(4, 3, '2026-04-04 12:00:00', 'Courtroom D', 4),
(5, 4, '2026-05-09 14:00:00', 'Courtroom A', 4),
(6, 5, '2026-05-15 10:45:00', 'Courtroom E', 4),
(7, 6, '2026-05-18 13:15:00', 'Courtroom F', 4);

INSERT INTO documents (id, case_id, file_url, file_name, mime_type, uploaded_by_user_id, uploaded_by_role, approval_status, is_public, is_official, rejection_reason, created_at) VALUES
(1, 1, 'C:/ccms-documents/turner/notice.pdf', 'notice.pdf', 'application/pdf', 1, 'Admin', 'APPROVED', TRUE, TRUE, NULL, '2026-05-01 09:00:00'),
(2, 1, 'C:/ccms-documents/turner/evidence-list.pdf', 'evidence-list.pdf', 'application/pdf', 1, 'Admin', 'APPROVED', TRUE, TRUE, NULL, '2026-05-01 09:05:00'),
(3, 2, 'C:/ccms-documents/riverside/appeal-summary.docx', 'appeal-summary.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1, 'Admin', 'APPROVED', TRUE, TRUE, NULL, '2026-05-01 09:10:00'),
(4, 4, 'C:/ccms-documents/cityworks/compliance-report.pdf', 'compliance-report.pdf', 'application/pdf', 1, 'Admin', 'APPROVED', TRUE, TRUE, NULL, '2026-05-01 09:15:00');
