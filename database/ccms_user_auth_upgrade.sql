USE ccms_db;

ALTER TABLE users
    MODIFY COLUMN role ENUM('Admin', 'Lawyer', 'Judge', 'Staff', 'Citizen') NOT NULL,
    ADD COLUMN IF NOT EXISTS mobile VARCHAR(15) NULL AFTER name,
    ADD COLUMN IF NOT EXISTS occupation VARCHAR(50) NULL AFTER role,
    ADD COLUMN IF NOT EXISTS bar_council_number VARCHAR(50) NULL AFTER occupation,
    ADD COLUMN IF NOT EXISTS court_id VARCHAR(50) NULL AFTER bar_council_number,
    ADD COLUMN IF NOT EXISTS aadhaar_number VARCHAR(20) NULL AFTER court_id,
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(255) NULL AFTER aadhaar_number,
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'Approved' AFTER profile_photo_url,
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(20) NOT NULL DEFAULT 'Available' AFTER approval_status;

ALTER TABLE cases
    ADD COLUMN IF NOT EXISTS lawyer_user_id INT NULL AFTER lawyer,
    ADD COLUMN IF NOT EXISTS judge_user_id INT NULL AFTER judge,
    ADD COLUMN IF NOT EXISTS court_details VARCHAR(160) NULL AFTER status;

ALTER TABLE hearings
    ADD COLUMN IF NOT EXISTS judge_user_id INT NULL AFTER courtroom;

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(200) NULL AFTER file_url,
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(120) NULL AFTER file_name,
    ADD COLUMN IF NOT EXISTS uploaded_by_user_id INT NULL AFTER mime_type,
    ADD COLUMN IF NOT EXISTS uploaded_by_role VARCHAR(40) NULL AFTER uploaded_by_user_id,
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER uploaded_by_role,
    ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT TRUE AFTER approval_status,
    ADD COLUMN IF NOT EXISTS is_official BOOLEAN NOT NULL DEFAULT TRUE AFTER is_public,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT NULL AFTER is_official,
    ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER rejection_reason;

ALTER TABLE admin_notifications
    ADD COLUMN IF NOT EXISTS target_role VARCHAR(40) NULL AFTER related_request_id,
    ADD COLUMN IF NOT EXISTS target_user_id INT NULL AFTER target_role,
    ADD COLUMN IF NOT EXISTS target_user_name VARCHAR(120) NULL AFTER target_user_id,
    ADD COLUMN IF NOT EXISTS created_by_user_id INT NULL AFTER target_user_name;

CREATE TABLE IF NOT EXISTS site_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_by_user_id INT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS change_requests (
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

CREATE TABLE IF NOT EXISTS approval_requests (
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

CREATE TABLE IF NOT EXISTS audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    user_name VARCHAR(120) NULL,
    role VARCHAR(40) NULL,
    action TEXT NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lawyer_notes (
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

INSERT INTO site_settings (setting_key, setting_value, updated_by_user_id) VALUES
('publicHomeTitle', 'Court Case Management System', NULL),
('publicHomeSummary', 'Public access to main judiciary information, updates, and guided case search.', NULL),
('publicHomeNotice', 'Detailed case access is available after login for citizens, lawyers, judges, admins, and staff.', NULL),
('dashboardNotice', 'Admin actions are saved immediately and remain under approval review until finalized.', NULL),
('aiReferenceNote', 'Add hearing notes, FIR extracts, timelines, or document text here for better answers.', NULL),
('aiBehaviorNote', 'Prefer concise, procedural, India-specific legal guidance and clearly separate facts, issues, and next steps.', NULL)
ON DUPLICATE KEY UPDATE setting_key = setting_key;
