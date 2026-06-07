package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.LawyerNote;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class LawyerNoteDAO {
    public LawyerNote create(LawyerNote note) throws SQLException {
        String sql = "INSERT INTO lawyer_notes (case_id, hearing_id, lawyer_user_id, lawyer_name, note_type, content, approval_status, rejection_reason) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindNoteForWrite(statement, note, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    note.setNoteId(keys.getInt(1));
                }
            }
        }
        return getById(note.getNoteId());
    }

    public LawyerNote restore(LawyerNote note) throws SQLException {
        String sql = "INSERT INTO lawyer_notes " +
                "(id, case_id, hearing_id, lawyer_user_id, lawyer_name, note_type, content, approval_status, rejection_reason, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE case_id = VALUES(case_id), hearing_id = VALUES(hearing_id), lawyer_user_id = VALUES(lawyer_user_id), " +
                "lawyer_name = VALUES(lawyer_name), note_type = VALUES(note_type), content = VALUES(content), approval_status = VALUES(approval_status), " +
                "rejection_reason = VALUES(rejection_reason), created_at = VALUES(created_at), updated_at = VALUES(updated_at)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, note.getNoteId());
            bindNoteForWrite(statement, note, true);
            statement.executeUpdate();
        }
        return getById(note.getNoteId());
    }

    public LawyerNote getById(int noteId) throws SQLException {
        String sql = "SELECT id, case_id, hearing_id, lawyer_user_id, lawyer_name, note_type, content, approval_status, rejection_reason, " +
                "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS updated_at " +
                "FROM lawyer_notes WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, noteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapNote(resultSet);
                }
            }
        }
        return null;
    }

    public List<LawyerNote> getNotesForLawyer(int lawyerUserId, Integer caseId) throws SQLException {
        String sql = "SELECT id, case_id, hearing_id, lawyer_user_id, lawyer_name, note_type, content, approval_status, rejection_reason, " +
                "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS updated_at " +
                "FROM lawyer_notes WHERE lawyer_user_id = ?" +
                (caseId != null ? " AND case_id = ?" : "") +
                " ORDER BY updated_at DESC, id DESC";

        List<LawyerNote> notes = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, lawyerUserId);
            if (caseId != null) {
                statement.setInt(2, caseId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notes.add(mapNote(resultSet));
                }
            }
        }
        return notes;
    }

    public List<LawyerNote> getNotesByCaseId(int caseId) throws SQLException {
        String sql = "SELECT id, case_id, hearing_id, lawyer_user_id, lawyer_name, note_type, content, approval_status, rejection_reason, " +
                "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS updated_at " +
                "FROM lawyer_notes WHERE case_id = ? ORDER BY updated_at DESC, id DESC";
        List<LawyerNote> notes = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notes.add(mapNote(resultSet));
                }
            }
        }
        return notes;
    }

    public boolean update(int noteId, LawyerNote note) throws SQLException {
        String sql = "UPDATE lawyer_notes SET case_id = ?, hearing_id = ?, note_type = ?, content = ?, approval_status = ?, rejection_reason = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, note.getCaseId());
            setNullableInteger(statement, 2, note.getHearingId());
            statement.setString(3, note.getNoteType());
            statement.setString(4, note.getContent());
            statement.setString(5, note.getApprovalStatus());
            setNullableString(statement, 6, note.getRejectionReason());
            statement.setInt(7, noteId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateApprovalStatus(int noteId, String approvalStatus, String rejectionReason) throws SQLException {
        String sql = "UPDATE lawyer_notes SET approval_status = ?, rejection_reason = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, approvalStatus);
            setNullableString(statement, 2, rejectionReason);
            statement.setInt(3, noteId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int noteId) throws SQLException {
        String sql = "DELETE FROM lawyer_notes WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, noteId);
            return statement.executeUpdate() > 0;
        }
    }

    private void bindNoteForWrite(PreparedStatement statement, LawyerNote note, boolean includeTimestamps) throws SQLException {
        int startIndex = includeTimestamps ? 2 : 1;
        statement.setInt(startIndex, note.getCaseId());
        setNullableInteger(statement, startIndex + 1, note.getHearingId());
        statement.setInt(startIndex + 2, note.getLawyerUserId());
        statement.setString(startIndex + 3, note.getLawyerName());
        statement.setString(startIndex + 4, note.getNoteType());
        statement.setString(startIndex + 5, note.getContent());
        statement.setString(startIndex + 6, note.getApprovalStatus());
        setNullableString(statement, startIndex + 7, note.getRejectionReason());
        if (includeTimestamps) {
            statement.setString(startIndex + 8, note.getCreatedAt());
            statement.setString(startIndex + 9, note.getUpdatedAt());
        }
    }

    private LawyerNote mapNote(ResultSet resultSet) throws SQLException {
        LawyerNote note = new LawyerNote();
        note.setNoteId(resultSet.getInt("id"));
        note.setCaseId(resultSet.getInt("case_id"));
        int hearingId = resultSet.getInt("hearing_id");
        note.setHearingId(resultSet.wasNull() ? null : hearingId);
        note.setLawyerUserId(resultSet.getInt("lawyer_user_id"));
        note.setLawyerName(resultSet.getString("lawyer_name"));
        note.setNoteType(resultSet.getString("note_type"));
        note.setContent(resultSet.getString("content"));
        note.setApprovalStatus(resultSet.getString("approval_status"));
        note.setRejectionReason(resultSet.getString("rejection_reason"));
        note.setCreatedAt(resultSet.getString("created_at"));
        note.setUpdatedAt(resultSet.getString("updated_at"));
        return note;
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value.trim());
    }
}
