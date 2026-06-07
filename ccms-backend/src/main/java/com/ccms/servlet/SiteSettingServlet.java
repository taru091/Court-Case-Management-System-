package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.SiteSettingDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.SiteSettingRecord;
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
import java.util.Set;

@WebServlet("/api/site-settings/*")
public class SiteSettingServlet extends HttpServlet {
    private final SiteSettingDAO siteSettingDAO = new SiteSettingDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private static final Set<String> ADMIN_ROLES = Set.of("Admin");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = request.getPathInfo();

        try {
            if ("/public".equals(path)) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, siteSettingDAO.getAll());
                return;
            }

            if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
                return;
            }

            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, siteSettingDAO.getAll());
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch site settings.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
            return;
        }

        String path = request.getPathInfo();
        if (path == null || "/".equals(path)) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Setting key is required.");
            return;
        }

        try {
            SettingInput input = ServletUtil.readJsonBody(request, SettingInput.class);
            if (input == null || isBlank(input.value)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Setting value is required.");
                return;
            }

            String key = path.startsWith("/") ? path.substring(1) : path;
            User admin = ServletUtil.getSessionUser(request);
            SiteSettingRecord before = siteSettingDAO.getByKey(key);
            SiteSettingRecord updated = siteSettingDAO.upsert(key, input.value.trim(), admin != null ? admin.getId() : null);

            ApprovalRequestRecord requestRecord = new ApprovalRequestRecord();
            requestRecord.setRequestType("ADMIN_SITE_SETTING_UPDATE");
            requestRecord.setRequestedByRole(admin != null ? admin.getRole() : "Admin");
            requestRecord.setRequestedByUser(admin != null ? admin.getId() : 0);
            requestRecord.setRequestedByName(admin != null ? admin.getName() : "Admin");
            requestRecord.setApprovalRole("Judge");
            requestRecord.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_SITE_SETTING);
            requestRecord.setTargetEntityId(null);
            requestRecord.setActionType(before == null ? ApprovalWorkflowUtil.ACTION_CREATE : ApprovalWorkflowUtil.ACTION_UPDATE);
            requestRecord.setRequestTitle("Site setting update for " + key);
            requestRecord.setRequestPayload(JsonUtil.getGson().toJson(updated));
            requestRecord.setBeforePayload(before == null ? null : JsonUtil.getGson().toJson(before));
            requestRecord.setAfterPayload(JsonUtil.getGson().toJson(updated));
            requestRecord.setLiveChangeApplied(true);
            requestRecord.setStatus("PENDING");

            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(requestRecord);
            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A site setting change is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin updated site setting " + key + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, updated);
        } catch (SQLException exception) {
            throw new ServletException("Unable to update site setting.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class SettingInput {
        private String value;
    }
}
