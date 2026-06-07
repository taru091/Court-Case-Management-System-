package com.ccms.config;

import com.ccms.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SchemaBootstrap {
    private static volatile boolean initialized = false;
    private static final List<DemoUserSeed> DEMO_USERS = List.of(
            new DemoUserSeed("admin", "Admin", "9999999991", "admin@ccms.com", "admin", "Admin", "Admin", null, null, "111122223333", "Available"),
            new DemoUserSeed("lawyer", "Lawyer", "9999999992", "lawyer@ccms.com", "lawyer123", "Lawyer", "Lawyer", "BCI-LAW-1001", null, "222233334444", "Available"),
            new DemoUserSeed("staff", "Staff", "9999999993", "staff@ccms.com", "staff123", "Staff", "Staff", null, null, "333344445555", "Available"),
            new DemoUserSeed("judge", "Judge", "9999999994", "judge@ccms.com", "judge123", "Judge", "Judge", null, "COURT-101", "444455556666", "Available"),
            new DemoUserSeed("citizen", "Citizen", "9999999995", "citizen@ccms.com", "citizen123", "Citizen", "Citizen", null, null, "555566667777", "Available")
    );

    private SchemaBootstrap() {
    }

    public static void initialize(Connection connection) throws SQLException {
        if (initialized) {
            return;
        }

        synchronized (SchemaBootstrap.class) {
            if (initialized) {
                return;
            }

            createTables(connection);
            upgradeLegacyUserTable(connection);
            upgradeWorkflowTables(connection);
            seedDefaults(connection);
            initialized = true;
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        execute(connection,
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "username VARCHAR(60) NOT NULL UNIQUE, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "mobile VARCHAR(15), " +
                        "email VARCHAR(120) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "role ENUM('Admin', 'Lawyer', 'Judge', 'Staff', 'Citizen') NOT NULL, " +
                        "occupation VARCHAR(50), " +
                        "bar_council_number VARCHAR(50), " +
                        "court_id VARCHAR(50), " +
                        "aadhaar_number VARCHAR(20), " +
                        "profile_photo_url VARCHAR(255), " +
                        "approval_status VARCHAR(20) NOT NULL DEFAULT 'Approved', " +
                        "availability_status VARCHAR(20) NOT NULL DEFAULT 'Available'" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS cases (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "case_name VARCHAR(150) NOT NULL, " +
                        "client VARCHAR(120) NOT NULL, " +
                        "lawyer VARCHAR(120) NOT NULL, " +
                        "lawyer_user_id INT NULL, " +
                        "judge VARCHAR(120) NOT NULL, " +
                        "judge_user_id INT NULL, " +
                        "status ENUM('Active', 'Pending', 'Closed') NOT NULL, " +
                        "court_details VARCHAR(160) NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS hearings (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "case_id INT NOT NULL, " +
                        "date DATETIME NOT NULL, " +
                        "courtroom VARCHAR(100) NOT NULL, " +
                        "judge_user_id INT NULL, " +
                        "CONSTRAINT fk_hearing_case FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS documents (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "case_id INT NOT NULL, " +
                        "file_url VARCHAR(255) NOT NULL, " +
                        "file_name VARCHAR(200) NULL, " +
                        "mime_type VARCHAR(120) NULL, " +
                        "uploaded_by_user_id INT NULL, " +
                        "uploaded_by_role VARCHAR(40) NULL, " +
                        "approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', " +
                        "is_public BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "is_official BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "rejection_reason TEXT NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_document_case FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS lawyer_notes (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "case_id INT NOT NULL, " +
                        "hearing_id INT NULL, " +
                        "lawyer_user_id INT NOT NULL, " +
                        "lawyer_name VARCHAR(120) NOT NULL, " +
                        "note_type VARCHAR(60) NOT NULL, " +
                        "content LONGTEXT NOT NULL, " +
                        "approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                        "rejection_reason TEXT NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_lawyer_note_case FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS admin_notifications (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "title VARCHAR(160) NOT NULL, " +
                        "message TEXT NOT NULL, " +
                        "category VARCHAR(40) NOT NULL, " +
                        "related_request_id INT NULL, " +
                        "target_role VARCHAR(40) NULL, " +
                        "target_user_id INT NULL, " +
                        "target_user_name VARCHAR(120) NULL, " +
                        "created_by_user_id INT NULL, " +
                        "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS site_settings (" +
                        "setting_key VARCHAR(120) PRIMARY KEY, " +
                        "setting_value TEXT NOT NULL, " +
                        "updated_by_user_id INT NULL, " +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS change_requests (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "entity_type VARCHAR(40) NOT NULL, " +
                        "action_type VARCHAR(20) NOT NULL, " +
                        "entity_id INT NULL, " +
                        "request_title VARCHAR(180) NOT NULL, " +
                        "request_payload LONGTEXT NOT NULL, " +
                        "status VARCHAR(40) NOT NULL, " +
                        "requested_by_user_id INT NOT NULL, " +
                        "requested_by_name VARCHAR(120) NOT NULL, " +
                        "requested_by_role VARCHAR(40) NOT NULL, " +
                        "judge_reviewer_id INT NULL, " +
                        "judge_reviewer_name VARCHAR(120) NULL, " +
                        "admin_reviewer_id INT NULL, " +
                        "admin_reviewer_name VARCHAR(120) NULL, " +
                        "review_note TEXT NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS approval_requests (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "request_type VARCHAR(80) NOT NULL, " +
                        "requested_by_role VARCHAR(40) NOT NULL, " +
                        "requested_by_user INT NOT NULL, " +
                        "requested_by_name VARCHAR(120) NOT NULL, " +
                        "approval_role VARCHAR(40) NOT NULL, " +
                        "target_entity_type VARCHAR(40) NOT NULL, " +
                        "target_entity_id INT NULL, " +
                        "action_type VARCHAR(20) NOT NULL, " +
                        "request_title VARCHAR(180) NOT NULL, " +
                        "request_payload LONGTEXT NULL, " +
                        "before_payload LONGTEXT NULL, " +
                        "after_payload LONGTEXT NULL, " +
                        "live_change_applied BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                        "rejection_reason TEXT NULL, " +
                        "reviewed_by_user_id INT NULL, " +
                        "reviewed_by_user_name VARCHAR(120) NULL, " +
                        "reviewed_by_role VARCHAR(40) NULL, " +
                        "review_note TEXT NULL, " +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "reviewed_at DATETIME NULL, " +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")");

        execute(connection,
                "CREATE TABLE IF NOT EXISTS audit_logs (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "user_id INT NULL, " +
                        "user_name VARCHAR(120) NULL, " +
                        "role VARCHAR(40) NULL, " +
                        "action TEXT NOT NULL, " +
                        "timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ")");
    }

    private static void upgradeLegacyUserTable(Connection connection) throws SQLException {
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN username VARCHAR(60) NULL AFTER id");
        execute(connection,
                "UPDATE users SET role = CASE UPPER(TRIM(role)) " +
                        "WHEN 'ADMIN' THEN 'Admin' " +
                        "WHEN 'LAWYER' THEN 'Lawyer' " +
                        "WHEN 'JUDGE' THEN 'Judge' " +
                        "WHEN 'STAFF' THEN 'Staff' " +
                        "WHEN 'CITIZEN' THEN 'Citizen' " +
                        "ELSE role END");
        execute(connection, "ALTER TABLE users MODIFY COLUMN role ENUM('Admin', 'Lawyer', 'Judge', 'Staff', 'Citizen') NOT NULL");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN mobile VARCHAR(15) NULL AFTER name");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN occupation VARCHAR(50) NULL AFTER role");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN bar_council_number VARCHAR(50) NULL AFTER occupation");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN court_id VARCHAR(50) NULL AFTER bar_council_number");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN aadhaar_number VARCHAR(20) NULL AFTER court_id");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD COLUMN profile_photo_url VARCHAR(255) NULL AFTER aadhaar_number");
        executeIgnoringAlreadyExists(connection,
                "ALTER TABLE users ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'Approved' AFTER profile_photo_url");
        executeIgnoringAlreadyExists(connection,
                "ALTER TABLE users ADD COLUMN availability_status VARCHAR(20) NOT NULL DEFAULT 'Available' AFTER approval_status");

        normalizeUsernames(connection);
        execute(connection, "UPDATE users SET approval_status = COALESCE(NULLIF(TRIM(approval_status), ''), 'Approved')");
        execute(connection, "UPDATE users SET availability_status = COALESCE(NULLIF(TRIM(availability_status), ''), 'Available')");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE users ADD UNIQUE INDEX uq_users_username (username)");
        execute(connection, "ALTER TABLE users MODIFY COLUMN username VARCHAR(60) NOT NULL");
    }

    private static void upgradeWorkflowTables(Connection connection) throws SQLException {
        executeIgnoringAlreadyExists(connection, "ALTER TABLE cases ADD COLUMN lawyer_user_id INT NULL AFTER lawyer");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE cases ADD COLUMN judge_user_id INT NULL AFTER judge");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE cases ADD COLUMN court_details VARCHAR(160) NULL AFTER status");

        executeIgnoringAlreadyExists(connection, "ALTER TABLE hearings ADD COLUMN judge_user_id INT NULL AFTER courtroom");

        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN file_name VARCHAR(200) NULL AFTER file_url");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN mime_type VARCHAR(120) NULL AFTER file_name");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN uploaded_by_user_id INT NULL AFTER mime_type");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN uploaded_by_role VARCHAR(40) NULL AFTER uploaded_by_user_id");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER uploaded_by_role");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE AFTER approval_status");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN is_official BOOLEAN NOT NULL DEFAULT TRUE AFTER is_public");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN rejection_reason TEXT NULL AFTER is_official");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE documents ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER rejection_reason");

        executeIgnoringAlreadyExists(connection, "ALTER TABLE admin_notifications ADD COLUMN target_role VARCHAR(40) NULL AFTER related_request_id");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE admin_notifications ADD COLUMN target_user_id INT NULL AFTER target_role");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE admin_notifications ADD COLUMN target_user_name VARCHAR(120) NULL AFTER target_user_id");
        executeIgnoringAlreadyExists(connection, "ALTER TABLE admin_notifications ADD COLUMN created_by_user_id INT NULL AFTER target_user_name");
        execute(connection, "UPDATE admin_notifications SET target_role = COALESCE(NULLIF(TRIM(target_role), ''), 'Admin')");
    }

    private static void seedDefaults(Connection connection) throws SQLException {
        seedDemoUsers(connection);
        seedSiteSettings(connection);

        Integer lawyerId = getUserIdByUsername(connection, "lawyer");
        Integer judgeId = getUserIdByUsername(connection, "judge");
        Integer adminId = getUserIdByUsername(connection, "admin");

        seedCases(connection, lawyerId, judgeId);
        seedHearings(connection, judgeId);
        seedDocuments(connection, adminId);
        backfillWorkflowColumns(connection, lawyerId, judgeId);
    }

    private static void seedSiteSettings(Connection connection) throws SQLException {
        execute(connection,
                "INSERT INTO site_settings (setting_key, setting_value, updated_by_user_id) VALUES " +
                        "('publicHomeTitle', 'Court Case Management System', NULL), " +
                        "('publicHomeSummary', 'Public access to main judiciary information, updates, and guided case search.', NULL), " +
                        "('publicHomeNotice', 'Detailed case access is available after login for citizens, lawyers, judges, admins, and staff.', NULL), " +
                        "('dashboardNotice', 'Admin actions are saved immediately and remain under approval review until finalized.', NULL), " +
                        "('aiReferenceNote', 'Add hearing notes, FIR extracts, timelines, or document text here for better answers.', NULL), " +
                        "('aiBehaviorNote', 'Prefer concise, procedural, India-specific legal guidance and clearly separate facts, issues, and next steps.', NULL) " +
                        "ON DUPLICATE KEY UPDATE setting_key = setting_key");
    }

    private static void seedCases(Connection connection, Integer lawyerId, Integer judgeId) throws SQLException {
        String sql = "INSERT INTO cases " +
                "(id, case_name, client, lawyer, lawyer_user_id, judge, judge_user_id, status, court_details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "case_name = VALUES(case_name), " +
                "client = VALUES(client), " +
                "lawyer = VALUES(lawyer), " +
                "lawyer_user_id = VALUES(lawyer_user_id), " +
                "judge = VALUES(judge), " +
                "judge_user_id = VALUES(judge_user_id), " +
                "status = VALUES(status), " +
                "court_details = VALUES(court_details), " +
                "created_at = VALUES(created_at)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            addSeedCase(statement, 1, "State vs Turner Holdings", "Avery Turner", "Lawyer", lawyerId, "Judge", judgeId, "Active", "District Court Hall A", "2026-04-10 10:30:00");
            addSeedCase(statement, 2, "Riverside Property Appeal", "Riverside Group", "Lawyer", lawyerId, "Judge", judgeId, "Pending", "District Court Hall B", "2026-04-18 14:15:00");
            addSeedCase(statement, 3, "Maya Foods Contract Review", "Maya Foods Ltd.", "Lawyer", lawyerId, "Judge", judgeId, "Closed", "District Court Hall C", "2026-03-28 12:00:00");
            addSeedCase(statement, 4, "City Works Compliance Matter", "City Works", "Lawyer", lawyerId, "Judge", judgeId, "Active", "District Court Hall A", "2026-04-25 16:10:00");
            addSeedCase(statement, 5, "Northwind Evidence Review", "Northwind Services", "Lawyer", lawyerId, "Judge", judgeId, "Pending", "District Court Hall E", "2026-04-30 09:20:00");
            addSeedCase(statement, 6, "Horizon Estate Settlement", "Laura Benton", "Lawyer", lawyerId, "Judge", judgeId, "Active", "District Court Hall F", "2026-05-01 08:50:00");
            statement.executeBatch();
        }
    }

    private static void addSeedCase(PreparedStatement statement,
                                    int id,
                                    String caseName,
                                    String client,
                                    String lawyerName,
                                    Integer lawyerUserId,
                                    String judgeName,
                                    Integer judgeUserId,
                                    String status,
                                    String courtDetails,
                                    String createdAt) throws SQLException {
        statement.setInt(1, id);
        statement.setString(2, caseName);
        statement.setString(3, client);
        statement.setString(4, lawyerName);
        setNullableInteger(statement, 5, lawyerUserId);
        statement.setString(6, judgeName);
        setNullableInteger(statement, 7, judgeUserId);
        statement.setString(8, status);
        statement.setString(9, courtDetails);
        statement.setString(10, createdAt);
        statement.addBatch();
    }

    private static void seedHearings(Connection connection, Integer judgeId) throws SQLException {
        String sql = "INSERT INTO hearings (id, case_id, date, courtroom, judge_user_id) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE case_id = VALUES(case_id), date = VALUES(date), courtroom = VALUES(courtroom), judge_user_id = VALUES(judge_user_id)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            addSeedHearing(statement, 1, 1, "2026-05-12 11:00:00", "Courtroom A", judgeId);
            addSeedHearing(statement, 2, 1, "2026-05-28 10:00:00", "Courtroom B", judgeId);
            addSeedHearing(statement, 3, 2, "2026-05-20 09:30:00", "Courtroom C", judgeId);
            addSeedHearing(statement, 4, 3, "2026-04-04 12:00:00", "Courtroom D", judgeId);
            addSeedHearing(statement, 5, 4, "2026-05-09 14:00:00", "Courtroom A", judgeId);
            addSeedHearing(statement, 6, 5, "2026-05-15 10:45:00", "Courtroom E", judgeId);
            addSeedHearing(statement, 7, 6, "2026-05-18 13:15:00", "Courtroom F", judgeId);
            statement.executeBatch();
        }
    }

    private static void addSeedHearing(PreparedStatement statement,
                                       int id,
                                       int caseId,
                                       String date,
                                       String courtroom,
                                       Integer judgeId) throws SQLException {
        statement.setInt(1, id);
        statement.setInt(2, caseId);
        statement.setString(3, date);
        statement.setString(4, courtroom);
        setNullableInteger(statement, 5, judgeId);
        statement.addBatch();
    }

    private static void seedDocuments(Connection connection, Integer adminId) throws SQLException {
        String sql = "INSERT INTO documents " +
                "(id, case_id, file_url, file_name, mime_type, uploaded_by_user_id, uploaded_by_role, approval_status, is_public, is_official, rejection_reason, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "case_id = VALUES(case_id), " +
                "file_url = VALUES(file_url), " +
                "file_name = VALUES(file_name), " +
                "mime_type = VALUES(mime_type), " +
                "uploaded_by_user_id = VALUES(uploaded_by_user_id), " +
                "uploaded_by_role = VALUES(uploaded_by_role), " +
                "approval_status = VALUES(approval_status), " +
                "is_public = VALUES(is_public), " +
                "is_official = VALUES(is_official), " +
                "rejection_reason = VALUES(rejection_reason), " +
                "created_at = VALUES(created_at)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            addSeedDocument(statement, 1, 1, "C:/ccms-documents/turner/notice.pdf", "notice.pdf", "application/pdf", adminId);
            addSeedDocument(statement, 2, 1, "C:/ccms-documents/turner/evidence-list.pdf", "evidence-list.pdf", "application/pdf", adminId);
            addSeedDocument(statement, 3, 2, "C:/ccms-documents/riverside/appeal-summary.docx", "appeal-summary.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", adminId);
            addSeedDocument(statement, 4, 4, "C:/ccms-documents/cityworks/compliance-report.pdf", "compliance-report.pdf", "application/pdf", adminId);
            statement.executeBatch();
        }
    }

    private static void addSeedDocument(PreparedStatement statement,
                                        int id,
                                        int caseId,
                                        String fileUrl,
                                        String fileName,
                                        String mimeType,
                                        Integer adminId) throws SQLException {
        statement.setInt(1, id);
        statement.setInt(2, caseId);
        statement.setString(3, fileUrl);
        statement.setString(4, fileName);
        statement.setString(5, mimeType);
        setNullableInteger(statement, 6, adminId);
        statement.setString(7, "Admin");
        statement.setString(8, "APPROVED");
        statement.setBoolean(9, true);
        statement.setBoolean(10, true);
        statement.setNull(11, Types.LONGVARCHAR);
        statement.setString(12, "2026-05-01 09:00:00");
        statement.addBatch();
    }

    private static void backfillWorkflowColumns(Connection connection, Integer lawyerId, Integer judgeId) throws SQLException {
        if (lawyerId != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE cases SET lawyer_user_id = ? WHERE lawyer_user_id IS NULL")) {
                statement.setInt(1, lawyerId);
                statement.executeUpdate();
            }
        }

        if (judgeId != null) {
            try (PreparedStatement caseStatement = connection.prepareStatement(
                    "UPDATE cases SET judge_user_id = ? WHERE judge_user_id IS NULL");
                 PreparedStatement hearingStatement = connection.prepareStatement(
                         "UPDATE hearings SET judge_user_id = ? WHERE judge_user_id IS NULL")) {
                caseStatement.setInt(1, judgeId);
                caseStatement.executeUpdate();
                hearingStatement.setInt(1, judgeId);
                hearingStatement.executeUpdate();
            }
        }

        execute(connection,
                "UPDATE cases SET court_details = COALESCE(NULLIF(TRIM(court_details), ''), " +
                        "CONCAT('Courtroom ', COALESCE((SELECT h.courtroom FROM hearings h WHERE h.case_id = cases.id ORDER BY h.date ASC LIMIT 1), 'A')))");

        execute(connection, "UPDATE documents SET file_name = COALESCE(NULLIF(TRIM(file_name), ''), SUBSTRING_INDEX(file_url, '/', -1))");
        execute(connection, "UPDATE documents SET approval_status = COALESCE(NULLIF(TRIM(approval_status), ''), 'APPROVED')");
        execute(connection, "UPDATE documents SET uploaded_by_role = COALESCE(NULLIF(TRIM(uploaded_by_role), ''), 'Admin')");
    }

    private static Integer getUserIdByUsername(Connection connection, String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        return null;
    }

    private static void normalizeUsernames(Connection connection) throws SQLException {
        List<UserNameRow> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, email, name FROM users ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new UserNameRow(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("email"),
                        resultSet.getString("name")
                ));
            }
        }

        Set<String> usedUsernames = new HashSet<>();
        List<UserNameUpdate> updates = new ArrayList<>();

        for (UserNameRow user : users) {
            String preferred = firstFilled(user.username, emailLocalPart(user.email), user.name, "user" + user.id);
            String candidate = nextAvailableUsername(preferred, user.id, usedUsernames);
            if (user.username == null || !candidate.equals(user.username)) {
                updates.add(new UserNameUpdate(user.id, candidate));
            }
        }

        if (updates.isEmpty()) {
            return;
        }

        try (PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE users SET username = ? WHERE id = ?")) {
            for (UserNameUpdate update : updates) {
                updateStatement.setString(1, update.username);
                updateStatement.setInt(2, update.id);
                updateStatement.addBatch();
            }
            updateStatement.executeBatch();
        }
    }

    private static String nextAvailableUsername(String value, int userId, Set<String> usedUsernames) {
        String normalizedBase = sanitizeUsername(value);
        if (normalizedBase == null) {
            normalizedBase = "user" + userId;
        }

        String candidate = normalizedBase;
        int suffix = 1;
        while (usedUsernames.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = normalizedBase + suffix;
            suffix++;
        }

        usedUsernames.add(candidate.toLowerCase(Locale.ROOT));
        return candidate;
    }

    private static String sanitizeUsername(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9._-]", "");

        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private static String emailLocalPart(String email) {
        if (email == null) {
            return null;
        }

        int separatorIndex = email.indexOf('@');
        if (separatorIndex <= 0) {
            return email;
        }

        return email.substring(0, separatorIndex);
    }

    private static String firstFilled(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void seedDemoUsers(Connection connection) throws SQLException {
        String sql = "INSERT INTO users " +
                "(username, name, mobile, email, password, role, occupation, bar_council_number, court_id, aadhaar_number, profile_photo_url, approval_status, availability_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "username = VALUES(username), " +
                "name = VALUES(name), " +
                "mobile = VALUES(mobile), " +
                "password = VALUES(password), " +
                "role = VALUES(role), " +
                "occupation = VALUES(occupation), " +
                "bar_council_number = VALUES(bar_council_number), " +
                "court_id = VALUES(court_id), " +
                "aadhaar_number = VALUES(aadhaar_number), " +
                "profile_photo_url = VALUES(profile_photo_url), " +
                "approval_status = VALUES(approval_status), " +
                "availability_status = VALUES(availability_status)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (DemoUserSeed user : DEMO_USERS) {
                statement.setString(1, user.username);
                statement.setString(2, user.name);
                statement.setString(3, user.mobile);
                statement.setString(4, user.email);
                statement.setString(5, PasswordUtil.hashPassword(user.password));
                statement.setString(6, user.role);
                statement.setString(7, user.occupation);
                setNullableString(statement, 8, user.barCouncilNumber);
                setNullableString(statement, 9, user.courtId);
                setNullableString(statement, 10, user.aadhaarNumber);
                statement.setString(11, null);
                statement.setString(12, "Approved");
                statement.setString(13, user.availabilityStatus);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value);
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private static void executeIgnoringAlreadyExists(Connection connection, String sql) throws SQLException {
        try {
            execute(connection, sql);
        } catch (SQLException ignored) {
            // Safe upgrade path for databases that already contain the column or index.
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static final class DemoUserSeed {
        private final String username;
        private final String name;
        private final String mobile;
        private final String email;
        private final String password;
        private final String role;
        private final String occupation;
        private final String barCouncilNumber;
        private final String courtId;
        private final String aadhaarNumber;
        private final String availabilityStatus;

        private DemoUserSeed(String username,
                             String name,
                             String mobile,
                             String email,
                             String password,
                             String role,
                             String occupation,
                             String barCouncilNumber,
                             String courtId,
                             String aadhaarNumber,
                             String availabilityStatus) {
            this.username = username;
            this.name = name;
            this.mobile = mobile;
            this.email = email;
            this.password = password;
            this.role = role;
            this.occupation = occupation;
            this.barCouncilNumber = barCouncilNumber;
            this.courtId = courtId;
            this.aadhaarNumber = aadhaarNumber;
            this.availabilityStatus = availabilityStatus;
        }
    }

    private static final class UserNameRow {
        private final int id;
        private final String username;
        private final String email;
        private final String name;

        private UserNameRow(int id, String username, String email, String name) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.name = name;
        }
    }

    private static final class UserNameUpdate {
        private final int id;
        private final String username;

        private UserNameUpdate(int id, String username) {
            this.id = id;
            this.username = username;
        }
    }
}
