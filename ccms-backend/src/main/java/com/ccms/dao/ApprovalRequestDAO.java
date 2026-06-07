package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ApprovalRequestDAO {
    public ApprovalRequestRecord create(ApprovalRequestRecord requestRecord) throws SQLException {
        String sql = "INSERT INTO approval_requests " +
                "(request_type, requested_by_role, requested_by_user, requested_by_name, approval_role, target_entity_type, target_entity_id, action_type, request_title, request_payload, before_payload, after_payload, live_change_applied, status, rejection_reason, reviewed_by_user_id, reviewed_by_user_name, reviewed_by_role, review_note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, requestRecord.getRequestType());
            statement.setString(2, requestRecord.getRequestedByRole());
            statement.setInt(3, requestRecord.getRequestedByUser());
            statement.setString(4, requestRecord.getRequestedByName());
            statement.setString(5, requestRecord.getApprovalRole());
            statement.setString(6, requestRecord.getTargetEntityType());
            setNullableInteger(statement, 7, requestRecord.getTargetEntityId());
            statement.setString(8, requestRecord.getActionType());
            statement.setString(9, requestRecord.getRequestTitle());
            setNullableString(statement, 10, requestRecord.getRequestPayload());
            setNullableString(statement, 11, requestRecord.getBeforePayload());
            setNullableString(statement, 12, requestRecord.getAfterPayload());
            statement.setBoolean(13, requestRecord.isLiveChangeApplied());
            statement.setString(14, defaultStatus(requestRecord.getStatus()));
            setNullableString(statement, 15, requestRecord.getRejectionReason());
            setNullableInteger(statement, 16, requestRecord.getReviewedByUserId());
            setNullableString(statement, 17, requestRecord.getReviewedByUserName());
            setNullableString(statement, 18, requestRecord.getReviewedByRole());
            setNullableString(statement, 19, requestRecord.getReviewNote());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    requestRecord.setId(keys.getInt(1));
                }
            }
        }

        return getById(requestRecord.getId());
    }

    public ApprovalRequestRecord getById(int requestId) throws SQLException {
        String sql = "SELECT * FROM approval_requests WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRequest(resultSet);
                }
            }
        }
        return null;
    }

    public List<ApprovalRequestRecord> getRequestsForUser(User user) throws SQLException {
        if (user == null) {
            return List.of();
        }

        if ("Admin".equals(user.getRole())) {
            String sql = "SELECT * FROM approval_requests WHERE approval_role = 'Admin' OR requested_by_role = 'Admin' ORDER BY created_at DESC, id DESC";
            return getBySql(sql, null);
        }

        if ("Judge".equals(user.getRole())) {
            String sql = "SELECT * FROM approval_requests WHERE approval_role = 'Judge' OR requested_by_user = ? ORDER BY created_at DESC, id DESC";
            return getBySql(sql, user.getId());
        }

        return getBySql("SELECT * FROM approval_requests WHERE requested_by_user = ? ORDER BY created_at DESC, id DESC", user.getId());
    }

    public List<ApprovalRequestRecord> getPendingForApprovalRole(String approvalRole) throws SQLException {
        return getBySql("SELECT * FROM approval_requests WHERE approval_role = ? AND status = 'PENDING' ORDER BY created_at DESC, id DESC", approvalRole);
    }

    public ApprovalRequestRecord updateReview(int requestId,
                                              User reviewer,
                                              String status,
                                              String rejectionReason,
                                              String reviewNote) throws SQLException {
        String sql = "UPDATE approval_requests SET status = ?, rejection_reason = ?, reviewed_by_user_id = ?, reviewed_by_user_name = ?, reviewed_by_role = ?, review_note = ?, reviewed_at = NOW() WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            setNullableString(statement, 2, rejectionReason);
            statement.setInt(3, reviewer.getId());
            statement.setString(4, reviewer.getName());
            statement.setString(5, reviewer.getRole());
            setNullableString(statement, 6, reviewNote);
            statement.setInt(7, requestId);
            statement.executeUpdate();
        }
        return getById(requestId);
    }

    private List<ApprovalRequestRecord> getBySql(String sql, Object parameter) throws SQLException {
        List<ApprovalRequestRecord> requests = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                if (parameter instanceof Integer) {
                    statement.setInt(1, (Integer) parameter);
                } else {
                    statement.setString(1, String.valueOf(parameter));
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRequest(resultSet));
                }
            }
        }
        return requests;
    }

    private ApprovalRequestRecord mapRequest(ResultSet resultSet) throws SQLException {
        ApprovalRequestRecord record = new ApprovalRequestRecord();
        record.setId(resultSet.getInt("id"));
        record.setRequestType(resultSet.getString("request_type"));
        record.setRequestedByRole(resultSet.getString("requested_by_role"));
        record.setRequestedByUser(resultSet.getInt("requested_by_user"));
        record.setRequestedByName(resultSet.getString("requested_by_name"));
        record.setApprovalRole(resultSet.getString("approval_role"));
        record.setTargetEntityType(resultSet.getString("target_entity_type"));
        int entityId = resultSet.getInt("target_entity_id");
        record.setTargetEntityId(resultSet.wasNull() ? null : entityId);
        record.setActionType(resultSet.getString("action_type"));
        record.setRequestTitle(resultSet.getString("request_title"));
        record.setRequestPayload(resultSet.getString("request_payload"));
        record.setBeforePayload(resultSet.getString("before_payload"));
        record.setAfterPayload(resultSet.getString("after_payload"));
        record.setLiveChangeApplied(resultSet.getBoolean("live_change_applied"));
        record.setStatus(resultSet.getString("status"));
        record.setRejectionReason(resultSet.getString("rejection_reason"));
        int reviewedByUserId = resultSet.getInt("reviewed_by_user_id");
        record.setReviewedByUserId(resultSet.wasNull() ? null : reviewedByUserId);
        record.setReviewedByUserName(resultSet.getString("reviewed_by_user_name"));
        record.setReviewedByRole(resultSet.getString("reviewed_by_role"));
        record.setReviewNote(resultSet.getString("review_note"));
        record.setCreatedAt(resultSet.getString("created_at"));
        record.setReviewedAt(resultSet.getString("reviewed_at"));
        record.setUpdatedAt(resultSet.getString("updated_at"));
        return record;
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

    private String defaultStatus(String value) {
        return value == null || value.trim().isEmpty() ? "PENDING" : value.trim();
    }
}
