package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.UserDAO;
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
import java.util.Set;

@WebServlet("/api/users/*")
public class UserServlet extends HttpServlet {
    private static final Set<String> ADMIN_ROLES = Set.of("Admin");
    private static final Set<String> JUDGE_ROLES = Set.of("Judge");
    private static final Set<String> INTERNAL_ROLES = Set.of("Admin", "Judge", "Lawyer", "Staff");
    private static final Set<String> ALLOWED_AVAILABILITY = Set.of("Available", "Busy", "In Hearing", "On Leave");
    private static final Set<String> ALLOWED_ROLES = Set.of("Admin", "Lawyer", "Judge", "Staff", "Citizen");
    private static final Set<String> ALLOWED_APPROVAL_STATUSES = Set.of("Approved", "Review Hold");

    private final UserDAO userDAO = new UserDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        String path = request.getPathInfo();

        try {
            if ("/judges".equals(path)) {
                User user = ServletUtil.getSessionUser(request);
                if (user == null || !INTERNAL_ROLES.contains(user.getRole())) {
                    ServletUtil.sendMessage(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission for this action.");
                    return;
                }
                boolean availableOnly = "true".equalsIgnoreCase(request.getParameter("availableOnly"));
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, userDAO.getJudges(availableOnly));
                return;
            }

            if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
                return;
            }

            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, userDAO.getAllUsers());
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch user data.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        String path = request.getPathInfo();
        try {
            if ("/me/availability".equals(path)) {
                if (!ServletUtil.requireRole(request, response, JUDGE_ROLES)) {
                    return;
                }

                AvailabilityInput input = ServletUtil.readJsonBody(request, AvailabilityInput.class);
                if (input == null || !ALLOWED_AVAILABILITY.contains(input.availabilityStatus)) {
                    ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Availability must be Available, Busy, In Hearing, or On Leave.");
                    return;
                }

                User judge = ServletUtil.getSessionUser(request);
                userDAO.updateAvailabilityStatus(judge.getId(), input.availabilityStatus);
                judge.setAvailabilityStatus(input.availabilityStatus);
                auditLogDAO.log(judge, "Judge updated availability to " + input.availabilityStatus);
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, userDAO.findById(judge.getId()));
                return;
            }

            if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
                return;
            }

            int userId = ServletUtil.parseIdFromPath(path);
            User before = userDAO.findById(userId);
            if (before == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "User not found.");
                return;
            }

            User input = ServletUtil.readJsonBody(request, User.class);
            if (input == null || isBlank(input.getName()) || !ALLOWED_ROLES.contains(input.getRole())
                    || !ALLOWED_APPROVAL_STATUSES.contains(input.getApprovalStatus())
                    || !ALLOWED_AVAILABILITY.contains(input.getAvailabilityStatus())) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Valid name, role, approval status, and availability status are required.");
                return;
            }

            User after = cloneManagedUser(before);
            after.setName(input.getName().trim());
            after.setRole(input.getRole().trim());
            after.setApprovalStatus(input.getApprovalStatus().trim());
            after.setAvailabilityStatus(input.getAvailabilityStatus().trim());
            after.setCourtId(input.getCourtId());

            userDAO.updateManagedUser(after);

            User admin = ServletUtil.getSessionUser(request);
            ApprovalRequestRecord requestRecord = buildApprovalRequest(admin, before, after);
            ApprovalRequestRecord created = approvalRequestDAO.create(requestRecord);
            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A user-management update is waiting for judge review.",
                    "ApprovalWorkflow",
                    created.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin updated user " + after.getUsername() + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, userDAO.findById(userId));
        } catch (SQLException exception) {
            throw new ServletException("Unable to update user.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid user id.");
        }
    }

    private ApprovalRequestRecord buildApprovalRequest(User admin, User before, User after) {
        ApprovalRequestRecord record = new ApprovalRequestRecord();
        record.setRequestType("ADMIN_USER_UPDATE");
        record.setRequestedByRole(admin != null ? admin.getRole() : "Admin");
        record.setRequestedByUser(admin != null ? admin.getId() : 0);
        record.setRequestedByName(admin != null ? admin.getName() : "Admin");
        record.setApprovalRole("Judge");
        record.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_USER);
        record.setTargetEntityId(after.getId());
        record.setActionType(ApprovalWorkflowUtil.ACTION_UPDATE);
        record.setRequestTitle("User update for " + after.getUsername());
        record.setRequestPayload(JsonUtil.getGson().toJson(after));
        record.setBeforePayload(JsonUtil.getGson().toJson(before));
        record.setAfterPayload(JsonUtil.getGson().toJson(after));
        record.setLiveChangeApplied(true);
        record.setStatus("PENDING");
        return record;
    }

    private User cloneManagedUser(User source) {
        User clone = new User();
        clone.setId(source.getId());
        clone.setUsername(source.getUsername());
        clone.setName(source.getName());
        clone.setMobile(source.getMobile());
        clone.setEmail(source.getEmail());
        clone.setRole(source.getRole());
        clone.setOccupation(source.getOccupation());
        clone.setBarCouncilNumber(source.getBarCouncilNumber());
        clone.setCourtId(source.getCourtId());
        clone.setAadhaarNumber(source.getAadhaarNumber());
        clone.setProfilePhotoUrl(source.getProfilePhotoUrl());
        clone.setApprovalStatus(source.getApprovalStatus());
        clone.setAvailabilityStatus(source.getAvailabilityStatus());
        return clone;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class AvailabilityInput {
        private String availabilityStatus;
    }
}
