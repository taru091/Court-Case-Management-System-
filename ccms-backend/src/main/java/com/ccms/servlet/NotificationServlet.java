package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.model.User;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/notifications/*")
public class NotificationServlet extends HttpServlet {
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            User user = ServletUtil.getSessionUser(request);
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, notificationDAO.getForUser(user));
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch notifications.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            int notificationId = ServletUtil.parseIdFromPath(request.getPathInfo());
            boolean updated = notificationDAO.markRead(notificationId);
            if (!updated) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Notification not found.");
                return;
            }

            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, Map.of("message", "Notification marked as read."));
        } catch (SQLException exception) {
            throw new ServletException("Unable to update notification.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid notification id.");
        }
    }
}
