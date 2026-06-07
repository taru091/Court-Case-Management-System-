package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditLogDAO {
    public void log(User user, String action) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, user_name, role, action) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user == null) {
                statement.setNull(1, Types.INTEGER);
                statement.setNull(2, Types.VARCHAR);
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setInt(1, user.getId());
                statement.setString(2, user.getName());
                statement.setString(3, user.getRole());
            }
            statement.setString(4, action);
            statement.executeUpdate();
        }
    }
}
