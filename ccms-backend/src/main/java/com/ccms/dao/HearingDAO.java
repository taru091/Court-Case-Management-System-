package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.Hearing;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HearingDAO {
    private static final String HEARING_SELECT_SQL =
            "SELECT h.id, h.case_id, c.case_name, DATE_FORMAT(h.date, '%Y-%m-%d %H:%i') AS hearing_date, " +
                    "h.courtroom, h.judge_user_id, COALESCE(j.name, c.judge) AS judge_name " +
                    "FROM hearings h " +
                    "JOIN cases c ON c.id = h.case_id " +
                    "LEFT JOIN users j ON j.id = h.judge_user_id";

    public Hearing addHearing(Hearing hearing) throws SQLException {
        String sql = "INSERT INTO hearings (case_id, date, courtroom, judge_user_id) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, hearing.getCaseId());
            statement.setString(2, hearing.getHearingDate());
            statement.setString(3, hearing.getCourtroom());
            setNullableInteger(statement, 4, hearing.getJudgeUserId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    hearing.setHearingId(keys.getInt(1));
                }
            }
        }
        return getHearingById(hearing.getHearingId());
    }

    public Hearing restoreHearing(Hearing hearing) throws SQLException {
        String sql = "INSERT INTO hearings (id, case_id, date, courtroom, judge_user_id) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE case_id = VALUES(case_id), date = VALUES(date), courtroom = VALUES(courtroom), judge_user_id = VALUES(judge_user_id)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hearing.getHearingId());
            statement.setInt(2, hearing.getCaseId());
            statement.setString(3, hearing.getHearingDate());
            statement.setString(4, hearing.getCourtroom());
            setNullableInteger(statement, 5, hearing.getJudgeUserId());
            statement.executeUpdate();
        }
        return getHearingById(hearing.getHearingId());
    }

    public boolean updateHearing(int hearingId, Hearing hearing) throws SQLException {
        String sql = "UPDATE hearings SET case_id = ?, date = ?, courtroom = ?, judge_user_id = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hearing.getCaseId());
            statement.setString(2, hearing.getHearingDate());
            statement.setString(3, hearing.getCourtroom());
            setNullableInteger(statement, 4, hearing.getJudgeUserId());
            statement.setInt(5, hearingId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteHearing(int hearingId) throws SQLException {
        String sql = "DELETE FROM hearings WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hearingId);
            return statement.executeUpdate() > 0;
        }
    }

    public Hearing getHearingById(int hearingId) throws SQLException {
        String sql = HEARING_SELECT_SQL + " WHERE h.id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hearingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapHearing(resultSet);
                }
            }
        }
        return null;
    }

    public List<Hearing> getHearingsByCaseId(int caseId) throws SQLException {
        String sql = HEARING_SELECT_SQL + " WHERE h.case_id = ? ORDER BY h.date ASC";
        return getHearingsBySql(sql, caseId);
    }

    public List<Hearing> getHearingsByCaseIdForUser(int caseId, User user) throws SQLException {
        if (user == null || !"Lawyer".equals(user.getRole())) {
            return getHearingsByCaseId(caseId);
        }

        String sql = HEARING_SELECT_SQL + " WHERE h.case_id = ? AND c.lawyer_user_id = ? ORDER BY h.date ASC";
        List<Hearing> hearings = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);
            statement.setInt(2, user.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    hearings.add(mapHearing(resultSet));
                }
            }
        }
        return hearings;
    }

    public List<Hearing> getHearingsForJudge(int judgeUserId) throws SQLException {
        String sql = HEARING_SELECT_SQL + " WHERE h.judge_user_id = ? ORDER BY h.date ASC";
        return getHearingsBySql(sql, judgeUserId);
    }

    private List<Hearing> getHearingsBySql(String sql, int id) throws SQLException {
        List<Hearing> hearings = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    hearings.add(mapHearing(resultSet));
                }
            }
        }
        return hearings;
    }

    private Hearing mapHearing(ResultSet resultSet) throws SQLException {
        Hearing hearing = new Hearing();
        hearing.setHearingId(resultSet.getInt("id"));
        hearing.setCaseId(resultSet.getInt("case_id"));
        hearing.setCaseName(resultSet.getString("case_name"));
        hearing.setHearingDate(resultSet.getString("hearing_date"));
        hearing.setCourtroom(resultSet.getString("courtroom"));
        int judgeUserId = resultSet.getInt("judge_user_id");
        hearing.setJudgeUserId(resultSet.wasNull() ? null : judgeUserId);
        hearing.setJudgeName(resultSet.getString("judge_name"));
        return hearing;
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }
}
