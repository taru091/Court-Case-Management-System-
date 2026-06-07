package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.AdminNotificationRecord;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AdminNotificationDAO {
    public AdminNotificationRecord create(String title,
                                          String message,
                                          String category,
                                          Integer relatedRequestId) throws SQLException {
        return createForRole(title, message, category, relatedRequestId, "Admin", null, null, null);
    }

    public AdminNotificationRecord createForRole(String title,
                                                 String message,
                                                 String category,
                                                 Integer relatedRequestId,
                                                 String targetRole,
                                                 Integer targetUserId,
                                                 String targetUserName,
                                                 Integer createdByUserId) throws SQLException {
        String sql = "INSERT INTO admin_notifications " +
                "(title, message, category, related_request_id, target_role, target_user_id, target_user_name, created_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        AdminNotificationRecord record = new AdminNotificationRecord();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, message);
            statement.setString(3, category);
            setNullableInteger(statement, 4, relatedRequestId);
            setNullableString(statement, 5, targetRole);
            setNullableInteger(statement, 6, targetUserId);
            setNullableString(statement, 7, targetUserName);
            setNullableInteger(statement, 8, createdByUserId);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    record.setId(keys.getInt(1));
                }
            }
        }

        return getById(record.getId());
    }

    public AdminNotificationRecord createForUser(String title,
                                                 String message,
                                                 String category,
                                                 Integer relatedRequestId,
                                                 User targetUser,
                                                 Integer createdByUserId) throws SQLException {
        return createForRole(
                title,
                message,
                category,
                relatedRequestId,
                targetUser != null ? targetUser.getRole() : null,
                targetUser != null ? targetUser.getId() : null,
                targetUser != null ? targetUser.getName() : null,
                createdByUserId
        );
    }

    public List<AdminNotificationRecord> getAll() throws SQLException {
        String sql = "SELECT id, title, message, category, related_request_id, target_role, target_user_id, target_user_name, created_by_user_id, is_read, " +
                "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at " +
                "FROM admin_notifications ORDER BY created_at DESC, id DESC";
        List<AdminNotificationRecord> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                notifications.add(mapNotification(resultSet));
            }
        }

        return notifications;
    }

    public List<AdminNotificationRecord> getForUser(User user) throws SQLException {
        if (user == null) {
            return List.of();
        }

        String sql = "SELECT id, title, message, category, related_request_id, target_role, target_user_id, target_user_name, created_by_user_id, is_read, " +
                "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at " +
                "FROM admin_notifications " +
                "WHERE (target_user_id IS NULL OR target_user_id = ?) AND (target_role IS NULL OR target_role = ? OR target_role = 'ALL') " +
                "ORDER BY created_at DESC, id DESC";
        List<AdminNotificationRecord> notifications = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, user.getId());
            statement.setString(2, user.getRole());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }
        }

        return notifications;
    }

    public boolean markRead(int notificationId) throws SQLException {
        String sql = "UPDATE admin_notifications SET is_read = TRUE WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            return statement.executeUpdate() > 0;
        }
    }

    public AdminNotificationRecord getById(int notificationId) throws SQLException {
        String sql = "SELECT id, title, message, category, related_request_id, target_role, target_user_id, target_user_name, created_by_user_id, is_read, " +
                "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at " +
                "FROM admin_notifications WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapNotification(resultSet);
                }
            }
        }
        return null;
    }

    public boolean delete(int notificationId) throws SQLException {
        String sql = "DELETE FROM admin_notifications WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            return statement.executeUpdate() > 0;
        }
    }

    private AdminNotificationRecord mapNotification(ResultSet resultSet) throws SQLException {
        AdminNotificationRecord record = new AdminNotificationRecord();
        record.setId(resultSet.getInt("id"));
        record.setTitle(resultSet.getString("title"));
        record.setMessage(resultSet.getString("message"));
        record.setCategory(resultSet.getString("category"));
        int relatedRequestId = resultSet.getInt("related_request_id");
        record.setRelatedRequestId(resultSet.wasNull() ? null : relatedRequestId);
        record.setTargetRole(resultSet.getString("target_role"));
        int targetUserId = resultSet.getInt("target_user_id");
        record.setTargetUserId(resultSet.wasNull() ? null : targetUserId);
        record.setTargetUserName(resultSet.getString("target_user_name"));
        int createdByUserId = resultSet.getInt("created_by_user_id");
        record.setCreatedByUserId(resultSet.wasNull() ? null : createdByUserId);
        record.setRead(resultSet.getBoolean("is_read"));
        record.setCreatedAt(resultSet.getString("created_at"));
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
}
