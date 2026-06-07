package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.model.AdminNotificationRecord;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.User;
import com.ccms.util.ApprovalWorkflowUtil;
import com.ccms.util.JsonUtil;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

@WebServlet("/api/admin-notifications/*")
public class AdminNotificationServlet extends HttpServlet {
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private static final Set<String> ADMIN_ROLES = Set.of("Admin");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
            return;
        }

        try {
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, notificationDAO.getAll());
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch admin notifications.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
            return;
        }

        try {
            NotificationInput input = ServletUtil.readJsonBody(request, NotificationInput.class);
            if (input == null || isBlank(input.title) || isBlank(input.message)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Title and message are required.");
                return;
            }

            User admin = ServletUtil.getSessionUser(request);
            AdminNotificationRecord created = notificationDAO.createForRole(
                    input.title.trim(),
                    input.message.trim(),
                    isBlank(input.category) ? "AdminUpdate" : input.category.trim(),
                    null,
                    isBlank(input.targetRole) ? "ALL" : input.targetRole.trim(),
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );

            ApprovalRequestRecord requestRecord = new ApprovalRequestRecord();
            requestRecord.setRequestType("ADMIN_NOTIFICATION_CREATE");
            requestRecord.setRequestedByRole(admin != null ? admin.getRole() : "Admin");
            requestRecord.setRequestedByUser(admin != null ? admin.getId() : 0);
            requestRecord.setRequestedByName(admin != null ? admin.getName() : "Admin");
            requestRecord.setApprovalRole("Judge");
            requestRecord.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_NOTIFICATION);
            requestRecord.setTargetEntityId(created.getId());
            requestRecord.setActionType(ApprovalWorkflowUtil.ACTION_CREATE);
            requestRecord.setRequestTitle("Admin notification publish request");
            requestRecord.setRequestPayload(JsonUtil.getGson().toJson(created));
            requestRecord.setBeforePayload(null);
            requestRecord.setAfterPayload(JsonUtil.getGson().toJson(created));
            requestRecord.setLiveChangeApplied(true);
            requestRecord.setStatus("PENDING");

            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(requestRecord);
            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A notification publish request is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin created notification and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, created);
        } catch (SQLException exception) {
            throw new ServletException("Unable to create admin notification.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
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
            throw new ServletException("Unable to update admin notification.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid notification id.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class NotificationInput {
        private String title;
        private String message;
        private String category;
        private String targetRole;
    }
}
