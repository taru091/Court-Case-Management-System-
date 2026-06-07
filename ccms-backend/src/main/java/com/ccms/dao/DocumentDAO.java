package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.CaseDocument;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {
    private static final String DOCUMENT_SELECT_SQL =
            "SELECT d.id, d.case_id, d.file_url, d.file_name, d.mime_type, d.uploaded_by_user_id, d.uploaded_by_role, " +
                    "d.approval_status, d.is_public, d.is_official, d.rejection_reason, " +
                    "DATE_FORMAT(d.created_at, '%Y-%m-%d %H:%i') AS created_at " +
                    "FROM documents d " +
                    "JOIN cases c ON c.id = d.case_id";

    public CaseDocument addDocument(CaseDocument document) throws SQLException {
        String sql = "INSERT INTO documents " +
                "(case_id, file_url, file_name, mime_type, uploaded_by_user_id, uploaded_by_role, approval_status, is_public, is_official, rejection_reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindDocumentForWrite(statement, document, false);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    document.setDocumentId(keys.getInt(1));
                }
            }
        }
        return getById(document.getDocumentId());
    }

    public CaseDocument restoreDocument(CaseDocument document) throws SQLException {
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
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, document.getDocumentId());
            bindDocumentForWrite(statement, document, true);
            statement.executeUpdate();
        }
        return getById(document.getDocumentId());
    }

    public CaseDocument getById(int documentId) throws SQLException {
        String sql = DOCUMENT_SELECT_SQL + " WHERE d.id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDocument(resultSet);
                }
            }
        }
        return null;
    }

    public List<CaseDocument> getDocumentsByCaseId(int caseId) throws SQLException {
        String sql = DOCUMENT_SELECT_SQL + " WHERE d.case_id = ? ORDER BY d.id DESC";
        return getDocumentsBySql(sql, caseId);
    }

    public List<CaseDocument> getDocumentsByCaseIdForUser(int caseId, User user) throws SQLException {
        if (user == null) {
            return List.of();
        }

        if ("Lawyer".equals(user.getRole())) {
            String sql = DOCUMENT_SELECT_SQL +
                    " WHERE d.case_id = ? AND ((c.lawyer_user_id = ? AND d.approval_status = 'APPROVED') OR d.uploaded_by_user_id = ?) " +
                    "ORDER BY d.id DESC";
            List<CaseDocument> documents = new ArrayList<>();
            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, caseId);
                statement.setInt(2, user.getId());
                statement.setInt(3, user.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        documents.add(mapDocument(resultSet));
                    }
                }
            }
            return documents;
        }

        return getDocumentsByCaseId(caseId);
    }

    public List<CaseDocument> getPublicDocumentsByCaseId(int caseId) throws SQLException {
        String sql = DOCUMENT_SELECT_SQL +
                " WHERE d.case_id = ? AND d.approval_status = 'APPROVED' AND d.is_public = TRUE AND d.is_official = TRUE ORDER BY d.id DESC";
        return getDocumentsBySql(sql, caseId);
    }

    public boolean updateApprovalStatus(int documentId, String approvalStatus, String rejectionReason, boolean officialDocument) throws SQLException {
        String sql = "UPDATE documents SET approval_status = ?, rejection_reason = ?, is_official = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, approvalStatus);
            setNullableString(statement, 2, rejectionReason);
            statement.setBoolean(3, officialDocument);
            statement.setInt(4, documentId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteDocument(int documentId) throws SQLException {
        String sql = "DELETE FROM documents WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            return statement.executeUpdate() > 0;
        }
    }

    private List<CaseDocument> getDocumentsBySql(String sql, int caseId) throws SQLException {
        List<CaseDocument> documents = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    documents.add(mapDocument(resultSet));
                }
            }
        }
        return documents;
    }

    private void bindDocumentForWrite(PreparedStatement statement, CaseDocument document, boolean includeCreatedAt) throws SQLException {
        int index = includeCreatedAt ? 2 : 1;
        statement.setInt(index, document.getCaseId());
        statement.setString(index + 1, document.getFileUrl());
        statement.setString(index + 2, document.getFileName());
        setNullableString(statement, index + 3, document.getMimeType());
        setNullableInteger(statement, index + 4, document.getUploadedByUserId());
        setNullableString(statement, index + 5, document.getUploadedByRole());
        statement.setString(index + 6, document.getApprovalStatus());
        statement.setBoolean(index + 7, document.isPublicDocument());
        statement.setBoolean(index + 8, document.isOfficialDocument());
        setNullableString(statement, index + 9, document.getRejectionReason());
        if (includeCreatedAt) {
            statement.setString(index + 10, document.getCreatedAt());
        }
    }

    private CaseDocument mapDocument(ResultSet resultSet) throws SQLException {
        CaseDocument document = new CaseDocument();
        document.setDocumentId(resultSet.getInt("id"));
        document.setCaseId(resultSet.getInt("case_id"));
        document.setFileUrl(resultSet.getString("file_url"));
        document.setFileName(resultSet.getString("file_name"));
        document.setMimeType(resultSet.getString("mime_type"));
        int uploadedByUserId = resultSet.getInt("uploaded_by_user_id");
        document.setUploadedByUserId(resultSet.wasNull() ? null : uploadedByUserId);
        document.setUploadedByRole(resultSet.getString("uploaded_by_role"));
        document.setApprovalStatus(resultSet.getString("approval_status"));
        document.setPublicDocument(resultSet.getBoolean("is_public"));
        document.setOfficialDocument(resultSet.getBoolean("is_official"));
        document.setRejectionReason(resultSet.getString("rejection_reason"));
        document.setCreatedAt(resultSet.getString("created_at"));
        return document;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value.trim());
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }
}
