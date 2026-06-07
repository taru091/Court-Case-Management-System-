package com.ccms.servlet;

import com.ccms.dao.DashboardDAO;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {
    private final DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, dashboardDAO.getDashboardStats());
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch dashboard data.", exception);
        }
    }
}
