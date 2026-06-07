package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.ChangeRequestRecord;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChangeRequestDAO {
    public ChangeRequestRecord create(ChangeRequestRecord requestRecord) throws SQLException {
        String sql = "INSERT INTO change_requests " +
                "(entity_type, action_type, entity_id, request_title, request_payload, status, requested_by_user_id, requested_by_name, requested_by_role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, requestRecord.getEntityType());
            statement.setString(2, requestRecord.getActionType());

            if (requestRecord.getEntityId() == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, requestRecord.getEntityId());
            }

            statement.setString(4, requestRecord.getRequestTitle());
            statement.setString(5, requestRecord.getRequestPayload());
            statement.setString(6, requestRecord.getStatus());
            statement.setInt(7, requestRecord.getRequestedByUserId());
            statement.setString(8, requestRecord.getRequestedByName());
            statement.setString(9, requestRecord.getRequestedByRole());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    requestRecord.setId(keys.getInt(1));
                }
            }
        }

        return getById(requestRecord.getId());
    }

    public List<ChangeRequestRecord> getRequestsForUser(User user) throws SQLException {
        if (user == null) {
            return List.of();
        }

        if ("Admin".equals(user.getRole())) {
            return getBySql("SELECT * FROM change_requests ORDER BY created_at DESC, id DESC", null);
        }

        if ("Judge".equals(user.getRole())) {
            return getBySql(
                    "SELECT * FROM change_requests WHERE status = 'PendingJudgeApproval' OR requested_by_user_id = ? ORDER BY created_at DESC, id DESC",
                    user.getId()
            );
        }

        return getBySql(
                "SELECT * FROM change_requests WHERE requested_by_user_id = ? ORDER BY created_at DESC, id DESC",
                user.getId()
        );
    }

    public ChangeRequestRecord getById(int requestId) throws SQLException {
        List<ChangeRequestRecord> matches = getBySql("SELECT * FROM change_requests WHERE id = ?", requestId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public ChangeRequestRecord updateJudgeReview(int requestId, User judge, String nextStatus, String note) throws SQLException {
        String sql = "UPDATE change_requests SET status = ?, judge_reviewer_id = ?, judge_reviewer_name = ?, review_note = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nextStatus);
            statement.setInt(2, judge.getId());
            statement.setString(3, judge.getName());
            statement.setString(4, note);
            statement.setInt(5, requestId);
            statement.executeUpdate();
        }
        return getById(requestId);
    }

    public ChangeRequestRecord updateAdminReview(int requestId, User admin, String nextStatus, String note) throws SQLException {
        String sql = "UPDATE change_requests SET status = ?, admin_reviewer_id = ?, admin_reviewer_name = ?, review_note = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nextStatus);
            statement.setInt(2, admin.getId());
            statement.setString(3, admin.getName());
            statement.setString(4, note);
            statement.setInt(5, requestId);
            statement.executeUpdate();
        }
        return getById(requestId);
    }

    private List<ChangeRequestRecord> getBySql(String sql, Integer userId) throws SQLException {
        List<ChangeRequestRecord> requests = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId != null) {
                statement.setInt(1, userId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRequest(resultSet));
                }
            }
        }

        return requests;
    }

    private ChangeRequestRecord mapRequest(ResultSet resultSet) throws SQLException {
        ChangeRequestRecord record = new ChangeRequestRecord();
        record.setId(resultSet.getInt("id"));
        record.setEntityType(resultSet.getString("entity_type"));
        record.setActionType(resultSet.getString("action_type"));
        int entityId = resultSet.getInt("entity_id");
        record.setEntityId(resultSet.wasNull() ? null : entityId);
        record.setRequestTitle(resultSet.getString("request_title"));
        record.setRequestPayload(resultSet.getString("request_payload"));
        record.setStatus(resultSet.getString("status"));
        record.setRequestedByUserId(resultSet.getInt("requested_by_user_id"));
        record.setRequestedByName(resultSet.getString("requested_by_name"));
        record.setRequestedByRole(resultSet.getString("requested_by_role"));
        int judgeReviewerId = resultSet.getInt("judge_reviewer_id");
        record.setJudgeReviewerId(resultSet.wasNull() ? null : judgeReviewerId);
        record.setJudgeReviewerName(resultSet.getString("judge_reviewer_name"));
        int adminReviewerId = resultSet.getInt("admin_reviewer_id");
        record.setAdminReviewerId(resultSet.wasNull() ? null : adminReviewerId);
        record.setAdminReviewerName(resultSet.getString("admin_reviewer_name"));
        record.setReviewNote(resultSet.getString("review_note"));
        record.setCreatedAt(resultSet.getString("created_at"));
        record.setUpdatedAt(resultSet.getString("updated_at"));
        return record;
    }
}
