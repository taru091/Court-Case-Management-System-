package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.DelayAnalytics;
import com.ccms.model.JudgeWorkload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsDAO {
    public Map<String, Integer> getStatusAnalytics() throws SQLException {
        String sql = "SELECT " +
                "SUM(CASE WHEN status = 'Active' THEN 1 ELSE 0 END) AS active_count, " +
                "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending_count, " +
                "SUM(CASE WHEN status = 'Closed' THEN 1 ELSE 0 END) AS closed_count " +
                "FROM cases";

        Map<String, Integer> result = new HashMap<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                result.put("Active", resultSet.getInt("active_count"));
                result.put("Pending", resultSet.getInt("pending_count"));
                result.put("Closed", resultSet.getInt("closed_count"));
            }
        }
        return result;
    }

    public DelayAnalytics getDelayAnalytics() throws SQLException {
        String sql = "SELECT " +
                "COUNT(*) AS delayed_cases, " +
                "COALESCE(AVG(DATEDIFF(CURDATE(), DATE(last_hearing_date))), 0) AS average_delay_days " +
                "FROM (" +
                "  SELECT c.id, MAX(h.date) AS last_hearing_date " +
                "  FROM cases c " +
                "  JOIN hearings h ON h.case_id = c.id " +
                "  WHERE c.status <> 'Closed' " +
                "  GROUP BY c.id " +
                "  HAVING MAX(h.date) < NOW()" +
                ") delayed_case_data";

        DelayAnalytics analytics = new DelayAnalytics();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                analytics.setDelayedCases(resultSet.getInt("delayed_cases"));
                analytics.setAverageDelayDays(resultSet.getDouble("average_delay_days"));
            }
        }

        return analytics;
    }

    public List<JudgeWorkload> getJudgeWorkloads() throws SQLException {
        String sql = "SELECT judge, COUNT(*) AS total_cases FROM cases GROUP BY judge ORDER BY total_cases DESC, judge ASC";
        List<JudgeWorkload> workloads = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                workloads.add(new JudgeWorkload(
                        resultSet.getString("judge"),
                        resultSet.getInt("total_cases")
                ));
            }
        }

        return workloads;
    }
}
