package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDAO {
    private static final String USER_SELECT_COLUMNS =
            "id, username, name, mobile, email, role, occupation, bar_council_number, court_id, aadhaar_number, " +
                    "profile_photo_url, approval_status, availability_status";
    private static final String USER_SELECT_COLUMNS_WITH_PASSWORD = USER_SELECT_COLUMNS + ", password";

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean mobileExists(String mobile) throws SQLException {
        String sql = "SELECT id FROM users WHERE mobile = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, mobile);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users " +
                "(username, name, mobile, email, password, role, occupation, bar_council_number, court_id, aadhaar_number, profile_photo_url, approval_status, availability_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            user.setUsername(resolveUniqueUsername(connection, user.getUsername(), user.getEmail(), user.getName()));

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getName());
            setNullableString(statement, 3, user.getMobile());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getPassword());
            statement.setString(6, user.getRole());
            setNullableString(statement, 7, user.getOccupation());
            setNullableString(statement, 8, user.getBarCouncilNumber());
            setNullableString(statement, 9, user.getCourtId());
            setNullableString(statement, 10, user.getAadhaarNumber());
            setNullableString(statement, 11, user.getProfilePhotoUrl());
            statement.setString(12, defaultApprovalStatus(user.getApprovalStatus()));
            statement.setString(13, defaultAvailabilityStatus(user.getAvailabilityStatus()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }

        user.setPassword(null);
        return user;
    }

    public User findByLoginIdentifier(String identifier) throws SQLException {
        String sql = "SELECT " + USER_SELECT_COLUMNS_WITH_PASSWORD + " FROM users " +
                "WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?) OR mobile = ? " +
                "ORDER BY CASE " +
                "WHEN LOWER(username) = LOWER(?) THEN 0 " +
                "WHEN LOWER(email) = LOWER(?) THEN 1 " +
                "ELSE 2 END LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            statement.setString(3, identifier);
            statement.setString(4, identifier);
            statement.setString(5, identifier);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet, true);
                }
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT " + USER_SELECT_COLUMNS + " FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet, false);
                }
            }
        }
        return null;
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT " + USER_SELECT_COLUMNS + " FROM users WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet, false);
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT " + USER_SELECT_COLUMNS + " FROM users ORDER BY role ASC, name ASC";
        List<User> users = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet, false));
            }
        }
        return users;
    }

    public List<User> getJudges(boolean availableOnly) throws SQLException {
        String sql = "SELECT " + USER_SELECT_COLUMNS + " FROM users WHERE role = 'Judge'" +
                (availableOnly ? " AND availability_status = 'Available'" : "") +
                " ORDER BY name ASC";
        List<User> judges = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                judges.add(mapUser(resultSet, false));
            }
        }
        return judges;
    }

    public boolean updatePasswordByEmail(String email, String hashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hashedPassword);
            statement.setString(2, email);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateAvailabilityStatus(int userId, String availabilityStatus) throws SQLException {
        String sql = "UPDATE users SET availability_status = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultAvailabilityStatus(availabilityStatus));
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateManagedUser(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, role = ?, approval_status = ?, availability_status = ?, court_id = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getRole());
            statement.setString(3, defaultApprovalStatus(user.getApprovalStatus()));
            statement.setString(4, defaultAvailabilityStatus(user.getAvailabilityStatus()));
            setNullableString(statement, 5, user.getCourtId());
            statement.setInt(6, user.getId());
            return statement.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet resultSet, boolean includePassword) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setUsername(resultSet.getString("username"));
        user.setName(resultSet.getString("name"));
        user.setMobile(resultSet.getString("mobile"));
        user.setEmail(resultSet.getString("email"));
        if (includePassword) {
            user.setPassword(resultSet.getString("password"));
        }
        user.setRole(resultSet.getString("role"));
        user.setOccupation(resultSet.getString("occupation"));
        user.setBarCouncilNumber(resultSet.getString("bar_council_number"));
        user.setCourtId(resultSet.getString("court_id"));
        user.setAadhaarNumber(resultSet.getString("aadhaar_number"));
        user.setProfilePhotoUrl(resultSet.getString("profile_photo_url"));
        user.setApprovalStatus(resultSet.getString("approval_status"));
        user.setAvailabilityStatus(resultSet.getString("availability_status"));
        return user;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
            return;
        }
        statement.setString(index, value.trim());
    }

    private String resolveUniqueUsername(Connection connection,
                                         String requestedUsername,
                                         String email,
                                         String name) throws SQLException {
        String baseUsername = normalizeUsername(firstFilled(requestedUsername, emailLocalPart(email), name));
        if (baseUsername == null) {
            baseUsername = "user";
        }

        String candidate = baseUsername;
        int suffix = 1;
        while (usernameExists(connection, candidate)) {
            candidate = baseUsername + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean usernameExists(Connection connection, String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String normalizeUsername(String value) {
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

    private String emailLocalPart(String email) {
        if (email == null) {
            return null;
        }

        int separatorIndex = email.indexOf('@');
        if (separatorIndex <= 0) {
            return email;
        }

        return email.substring(0, separatorIndex);
    }

    private String firstFilled(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String defaultApprovalStatus(String value) {
        return value == null || value.trim().isEmpty() ? "Approved" : value.trim();
    }

    private String defaultAvailabilityStatus(String value) {
        return value == null || value.trim().isEmpty() ? "Available" : value.trim();
    }
}
