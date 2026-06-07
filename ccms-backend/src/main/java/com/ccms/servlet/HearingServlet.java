package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.CaseDAO;
import com.ccms.dao.HearingDAO;
import com.ccms.dao.UserDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.CaseRecord;
import com.ccms.model.Hearing;
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

@WebServlet("/api/hearings/*")
public class HearingServlet extends HttpServlet {
    private static final Set<String> HEARING_EDIT_ROLES = Set.of("Admin");

    private final HearingDAO hearingDAO = new HearingDAO();
    private final CaseDAO caseDAO = new CaseDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            int caseId = ServletUtil.parseIdFromPath(request.getPathInfo());
            User user = ServletUtil.getSessionUser(request);
            if (user != null && "Lawyer".equals(user.getRole())) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, hearingDAO.getHearingsByCaseIdForUser(caseId, user));
                return;
            }
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, hearingDAO.getHearingsByCaseId(caseId));
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch hearings.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, HEARING_EDIT_ROLES)) {
            return;
        }

        try {
            Hearing hearing = ServletUtil.readJsonBody(request, Hearing.class);
            Hearing normalized = normalizeHearingForWrite(hearing);
            if (!isValidHearing(normalized)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Case id, hearing date, courtroom, and judge are required.");
                return;
            }

            validateJudgeAvailability(normalized.getJudgeUserId());

            Hearing createdHearing = hearingDAO.addHearing(normalized);
            User admin = ServletUtil.getSessionUser(request);
            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(buildApprovalRequest(
                    admin,
                    null,
                    createdHearing,
                    ApprovalWorkflowUtil.ACTION_CREATE,
                    "Admin hearing schedule for case #" + createdHearing.getCaseId()
            ));

            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A hearing schedule is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin scheduled hearing #" + createdHearing.getHearingId() + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, createdHearing);
        } catch (SQLException exception) {
            throw new ServletException("Unable to add hearing.", exception);
        } catch (IllegalStateException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, HEARING_EDIT_ROLES)) {
            return;
        }

        try {
            int hearingId = ServletUtil.parseIdFromPath(request.getPathInfo());
            Hearing before = hearingDAO.getHearingById(hearingId);
            if (before == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Hearing not found.");
                return;
            }

            Hearing hearing = ServletUtil.readJsonBody(request, Hearing.class);
            Hearing normalized = normalizeHearingForWrite(hearing);
            if (!isValidHearing(normalized)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Case id, hearing date, courtroom, and judge are required.");
                return;
            }

            validateJudgeAvailability(normalized.getJudgeUserId());

            hearingDAO.updateHearing(hearingId, normalized);
            Hearing after = hearingDAO.getHearingById(hearingId);
            User admin = ServletUtil.getSessionUser(request);
            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(buildApprovalRequest(
                    admin,
                    before,
                    after,
                    ApprovalWorkflowUtil.ACTION_UPDATE,
                    "Admin hearing update for case #" + after.getCaseId()
            ));

            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A hearing update is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin updated hearing #" + hearingId + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, after);
        } catch (SQLException exception) {
            throw new ServletException("Unable to update hearing.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid hearing id.");
        } catch (IllegalStateException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        }
    }

    private Hearing normalizeHearingForWrite(Hearing hearing) throws SQLException {
        if (hearing == null) {
            return null;
        }

        Hearing normalized = new Hearing();
        normalized.setCaseId(hearing.getCaseId());
        normalized.setCourtroom(hearing.getCourtroom());
        normalized.setHearingDate(hearing.getHearingDate() == null ? null : hearing.getHearingDate().replace("T", " "));
        normalized.setJudgeUserId(hearing.getJudgeUserId());

        if (normalized.getJudgeUserId() == null && hearing.getCaseId() > 0) {
            CaseRecord caseRecord = caseDAO.getCaseById(hearing.getCaseId());
            if (caseRecord != null) {
                normalized.setJudgeUserId(caseRecord.getJudgeUserId());
            }
        }

        return normalized;
    }

    private void validateJudgeAvailability(Integer judgeUserId) throws SQLException {
        if (judgeUserId == null) {
            throw new IllegalStateException("Judge unavailable for new hearing assignment.");
        }

        User judge = userDAO.findById(judgeUserId);
        if (judge == null || !"Judge".equals(judge.getRole()) || !"Available".equals(judge.getAvailabilityStatus())) {
            throw new IllegalStateException("Judge unavailable for new hearing assignment.");
        }
    }

    private ApprovalRequestRecord buildApprovalRequest(User admin,
                                                       Hearing before,
                                                       Hearing after,
                                                       String actionType,
                                                       String title) {
        ApprovalRequestRecord record = new ApprovalRequestRecord();
        record.setRequestType("ADMIN_HEARING_" + actionType);
        record.setRequestedByRole(admin != null ? admin.getRole() : "Admin");
        record.setRequestedByUser(admin != null ? admin.getId() : 0);
        record.setRequestedByName(admin != null ? admin.getName() : "Admin");
        record.setApprovalRole("Judge");
        record.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_HEARING);
        record.setTargetEntityId(after != null ? after.getHearingId() : (before != null ? before.getHearingId() : null));
        record.setActionType(actionType);
        record.setRequestTitle(title);
        record.setRequestPayload(JsonUtil.getGson().toJson(after != null ? after : before));
        record.setBeforePayload(before == null ? null : JsonUtil.getGson().toJson(before));
        record.setAfterPayload(after == null ? null : JsonUtil.getGson().toJson(after));
        record.setLiveChangeApplied(true);
        record.setStatus("PENDING");
        return record;
    }

    private boolean isValidHearing(Hearing hearing) {
        return hearing != null
                && hearing.getCaseId() > 0
                && !isBlank(hearing.getHearingDate())
                && !isBlank(hearing.getCourtroom())
                && hearing.getJudgeUserId() != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
