package com.ccms.dao;

import com.ccms.config.DBConnection;
import com.ccms.model.CaseRecord;
import com.ccms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CaseDAO {
    private static final String CASE_SELECT_SQL =
            "SELECT c.id, c.case_name, c.client, c.lawyer, c.lawyer_user_id, c.judge, c.judge_user_id, c.status, c.court_details, " +
                    "DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i') AS created_at, " +
                    "DATE_FORMAT((SELECT h.date FROM hearings h WHERE h.case_id = c.id AND h.date >= NOW() ORDER BY h.date ASC LIMIT 1), '%Y-%m-%d %H:%i') AS next_hearing_date, " +
                    "(SELECT h.courtroom FROM hearings h WHERE h.case_id = c.id AND h.date >= NOW() ORDER BY h.date ASC LIMIT 1) AS next_courtroom " +
                    "FROM cases c";

    public List<CaseRecord> getAllCases() throws SQLException {
        return getCasesForUser(null, null, null);
    }

    public List<CaseRecord> getCasesForUser(User user, String query, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(CASE_SELECT_SQL);
        List<Object> parameters = new ArrayList<>();
        boolean hasWhere = false;

        if (user != null && "Lawyer".equals(user.getRole())) {
            sql.append(" WHERE c.lawyer_user_id = ?");
            parameters.add(user.getId());
            hasWhere = true;
        }

        if (query != null && !query.trim().isEmpty()) {
            sql.append(hasWhere ? " AND " : " WHERE ");
            sql.append("(CAST(c.id AS CHAR) LIKE ? OR c.case_name LIKE ? OR c.client LIKE ? OR c.lawyer LIKE ? OR c.judge LIKE ? OR c.status LIKE ? OR c.court_details LIKE ?)");
            String searchValue = "%" + query.trim() + "%";
            for (int index = 0; index < 7; index++) {
                parameters.add(searchValue);
            }
            hasWhere = true;
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append(hasWhere ? " AND " : " WHERE ");
            sql.append("c.status = ?");
            parameters.add(status.trim());
        }

        sql.append(" ORDER BY c.created_at DESC");

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapCaseList(resultSet);
            }
        }
    }

    public List<CaseRecord> searchCases(String query) throws SQLException {
        return getCasesForUser(null, query, null);
    }

    public List<CaseRecord> filterCasesByStatus(String status) throws SQLException {
        return getCasesForUser(null, null, status);
    }

    public CaseRecord createCase(CaseRecord caseRecord) throws SQLException {
        String sql = "INSERT INTO cases (case_name, client, lawyer, lawyer_user_id, judge, judge_user_id, status, court_details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindCaseForWrite(statement, caseRecord, false);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    caseRecord.setCaseId(keys.getInt(1));
                }
            }
        }

        return getCaseById(caseRecord.getCaseId());
    }

    public CaseRecord restoreCase(CaseRecord caseRecord) throws SQLException {
        String sql = "INSERT INTO cases (id, case_name, client, lawyer, lawyer_user_id, judge, judge_user_id, status, court_details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "case_name = VALUES(case_name), " +
                "client = VALUES(client), " +
                "lawyer = VALUES(lawyer), " +
                "lawyer_user_id = VALUES(lawyer_user_id), " +
                "judge = VALUES(judge), " +
                "judge_user_id = VALUES(judge_user_id), " +
                "status = VALUES(status), " +
                "court_details = VALUES(court_details), " +
                "created_at = VALUES(created_at)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseRecord.getCaseId());
            statement.setString(2, caseRecord.getCaseName());
            statement.setString(3, caseRecord.getClientName());
            statement.setString(4, caseRecord.getLawyerName());
            setNullableInteger(statement, 5, caseRecord.getLawyerUserId());
            statement.setString(6, caseRecord.getJudgeName());
            setNullableInteger(statement, 7, caseRecord.getJudgeUserId());
            statement.setString(8, caseRecord.getStatus());
            statement.setString(9, caseRecord.getCourtDetails());
            statement.setString(10, caseRecord.getCreatedAt());
            statement.executeUpdate();
        }

        return getCaseById(caseRecord.getCaseId());
    }

    public boolean updateCase(int caseId, CaseRecord caseRecord) throws SQLException {
        String sql = "UPDATE cases SET case_name = ?, client = ?, lawyer = ?, lawyer_user_id = ?, judge = ?, judge_user_id = ?, status = ?, court_details = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCaseForWrite(statement, caseRecord, false);
            statement.setInt(9, caseId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteCase(int caseId) throws SQLException {
        String sql = "DELETE FROM cases WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);
            return statement.executeUpdate() > 0;
        }
    }

    public CaseRecord getCaseById(int caseId) throws SQLException {
        String sql = CASE_SELECT_SQL + " WHERE c.id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCase(resultSet);
                }
            }
        }
        return null;
    }

    private void bindCaseForWrite(PreparedStatement statement, CaseRecord caseRecord, boolean includeCreatedAt) throws SQLException {
        statement.setString(1, caseRecord.getCaseName());
        statement.setString(2, caseRecord.getClientName());
        statement.setString(3, caseRecord.getLawyerName());
        setNullableInteger(statement, 4, caseRecord.getLawyerUserId());
        statement.setString(5, caseRecord.getJudgeName());
        setNullableInteger(statement, 6, caseRecord.getJudgeUserId());
        statement.setString(7, caseRecord.getStatus());
        statement.setString(8, caseRecord.getCourtDetails());
        if (includeCreatedAt) {
            statement.setString(9, caseRecord.getCreatedAt());
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object value = parameters.get(index);
            if (value instanceof Integer) {
                statement.setInt(index + 1, (Integer) value);
                continue;
            }
            statement.setString(index + 1, String.valueOf(value));
        }
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private List<CaseRecord> mapCaseList(ResultSet resultSet) throws SQLException {
        List<CaseRecord> cases = new ArrayList<>();
        while (resultSet.next()) {
            cases.add(mapCase(resultSet));
        }
        return cases;
    }

    private CaseRecord mapCase(ResultSet resultSet) throws SQLException {
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.setCaseId(resultSet.getInt("id"));
        caseRecord.setCaseName(resultSet.getString("case_name"));
        caseRecord.setClientName(resultSet.getString("client"));
        caseRecord.setLawyerName(resultSet.getString("lawyer"));
        int lawyerUserId = resultSet.getInt("lawyer_user_id");
        caseRecord.setLawyerUserId(resultSet.wasNull() ? null : lawyerUserId);
        caseRecord.setJudgeName(resultSet.getString("judge"));
        int judgeUserId = resultSet.getInt("judge_user_id");
        caseRecord.setJudgeUserId(resultSet.wasNull() ? null : judgeUserId);
        caseRecord.setStatus(resultSet.getString("status"));
        caseRecord.setCourtDetails(resultSet.getString("court_details"));
        caseRecord.setCreatedAt(resultSet.getString("created_at"));
        caseRecord.setNextHearingDate(resultSet.getString("next_hearing_date"));
        caseRecord.setNextCourtroom(resultSet.getString("next_courtroom"));
        return caseRecord;
    }
}
