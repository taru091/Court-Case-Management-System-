package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.DashboardStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDAO {
    public DashboardStats getDashboardStats() throws SQLException {
        String sql = "SELECT " +
                "COUNT(*) AS total_cases, " +
                "SUM(CASE WHEN status = 'Active' THEN 1 ELSE 0 END) AS active_cases, " +
                "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending_cases, " +
                "SUM(CASE WHEN status = 'Closed' THEN 1 ELSE 0 END) AS closed_cases " +
                "FROM cases";

        String hearingSql = "SELECT COUNT(*) AS upcoming_hearings FROM hearings WHERE date >= NOW()";

        DashboardStats stats = new DashboardStats();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement caseStatement = connection.prepareStatement(sql);
             PreparedStatement hearingStatement = connection.prepareStatement(hearingSql);
             ResultSet caseResult = caseStatement.executeQuery();
             ResultSet hearingResult = hearingStatement.executeQuery()) {

            if (caseResult.next()) {
                stats.setTotalCases(caseResult.getInt("total_cases"));
                stats.setActiveCases(caseResult.getInt("active_cases"));
                stats.setPendingCases(caseResult.getInt("pending_cases"));
                stats.setClosedCases(caseResult.getInt("closed_cases"));
            }

            if (hearingResult.next()) {
                stats.setUpcomingHearings(hearingResult.getInt("upcoming_hearings"));
            }
        }

        return stats;
    }
}
