package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.SiteSettingRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SiteSettingDAO {
    public List<SiteSettingRecord> getAll() throws SQLException {
        String sql = "SELECT setting_key, setting_value, updated_by_user_id, " +
                "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS updated_at " +
                "FROM site_settings ORDER BY setting_key ASC";
        List<SiteSettingRecord> settings = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                settings.add(mapSetting(resultSet));
            }
        }

        return settings;
    }

    public SiteSettingRecord getByKey(String key) throws SQLException {
        String sql = "SELECT setting_key, setting_value, updated_by_user_id, " +
                "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i') AS updated_at " +
                "FROM site_settings WHERE setting_key = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSetting(resultSet);
                }
            }
        }
        return null;
    }

    public SiteSettingRecord upsert(String key, String value, Integer updatedByUserId) throws SQLException {
        String sql = "INSERT INTO site_settings (setting_key, setting_value, updated_by_user_id) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_by_user_id = VALUES(updated_by_user_id)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);

            if (updatedByUserId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, updatedByUserId);
            }

            statement.executeUpdate();
        }

        return getByKey(key);
    }

    public boolean delete(String key) throws SQLException {
        String sql = "DELETE FROM site_settings WHERE setting_key = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            return statement.executeUpdate() > 0;
        }
    }

    private SiteSettingRecord mapSetting(ResultSet resultSet) throws SQLException {
        SiteSettingRecord record = new SiteSettingRecord();
        record.setKey(resultSet.getString("setting_key"));
        record.setValue(resultSet.getString("setting_value"));
        int updatedByUserId = resultSet.getInt("updated_by_user_id");
        record.setUpdatedByUserId(resultSet.wasNull() ? null : updatedByUserId);
        record.setUpdatedAt(resultSet.getString("updated_at"));
        return record;
    }
}
