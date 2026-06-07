package com.ccms.servlet;

import com.ccms.dao.AnalyticsDAO;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/analytics/*")
public class AnalyticsServlet extends HttpServlet {
    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        String path = request.getPathInfo();

        try {
            if ("/status".equals(path)) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, analyticsDAO.getStatusAnalytics());
                return;
            }

            if ("/delay".equals(path)) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, analyticsDAO.getDelayAnalytics());
                return;
            }

            if ("/judge-load".equals(path)) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, analyticsDAO.getJudgeWorkloads());
                return;
            }

            ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Analytics endpoint not found.");
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch analytics data.", exception);
        }
    }
}
