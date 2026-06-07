package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.CaseDAO;
import com.ccms.dao.DocumentDAO;
import com.ccms.dao.HearingDAO;
import com.ccms.dao.LawyerNoteDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.CaseRecord;
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

@WebServlet("/api/cases/*")
public class CaseServlet extends HttpServlet {
    private static final Set<String> CASE_EDIT_ROLES = Set.of("Admin");
    private static final Set<String> ALLOWED_STATUSES = Set.of("Active", "Pending", "Closed");

    private final CaseDAO caseDAO = new CaseDAO();
    private final HearingDAO hearingDAO = new HearingDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final LawyerNoteDAO lawyerNoteDAO = new LawyerNoteDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            User user = ServletUtil.getSessionUser(request);
            String path = request.getPathInfo();

            if (path == null || "/".equals(path)) {
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, caseDAO.getCasesForUser(user, null, null));
                return;
            }

            if ("/search".equals(path)) {
                String query = request.getParameter("query");
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, caseDAO.getCasesForUser(user, query, null));
                return;
            }

            if ("/filter".equals(path)) {
                String status = request.getParameter("status");
                if (status == null || !ALLOWED_STATUSES.contains(status)) {
                    ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Status must be Active, Pending, or Closed.");
                    return;
                }
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, caseDAO.getCasesForUser(user, null, status));
                return;
            }

            int caseId = ServletUtil.parseIdFromPath(path);
            CaseRecord caseRecord = caseDAO.getCaseById(caseId);
            if (caseRecord == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Case not found.");
                return;
            }

            if (user != null && "Lawyer".equals(user.getRole()) && (caseRecord.getLawyerUserId() == null || caseRecord.getLawyerUserId() != user.getId())) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to view this case.");
                return;
            }

            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, caseRecord);
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch case data.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, CASE_EDIT_ROLES)) {
            return;
        }

        try {
            CaseRecord caseRecord = ServletUtil.readJsonBody(request, CaseRecord.class);
            if (!isValidCase(caseRecord)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Please provide case name, client, lawyer, judge, and a valid status.");
                return;
            }

            CaseRecord createdCase = caseDAO.createCase(caseRecord);
            User admin = ServletUtil.getSessionUser(request);
            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(buildApprovalRequest(
                    admin,
                    null,
                    createdCase,
                    ApprovalWorkflowUtil.ACTION_CREATE,
                    "Admin case creation for " + createdCase.getCaseName()
            ));

            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A case creation is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin created case " + createdCase.getCaseName() + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, createdCase);
        } catch (SQLException exception) {
            throw new ServletException("Unable to create case.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, CASE_EDIT_ROLES)) {
            return;
        }

        try {
            int caseId = ServletUtil.parseIdFromPath(request.getPathInfo());
            CaseRecord before = caseDAO.getCaseById(caseId);
            if (before == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Case not found.");
                return;
            }

            CaseRecord caseRecord = ServletUtil.readJsonBody(request, CaseRecord.class);
            if (!isValidCase(caseRecord)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Please provide case name, client, lawyer, judge, and a valid status.");
                return;
            }

            caseDAO.updateCase(caseId, caseRecord);
            CaseRecord after = caseDAO.getCaseById(caseId);
            User admin = ServletUtil.getSessionUser(request);
            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(buildApprovalRequest(
                    admin,
                    before,
                    after,
                    ApprovalWorkflowUtil.ACTION_UPDATE,
                    "Admin case update for " + after.getCaseName()
            ));

            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A case update is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin updated case #" + caseId + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, after);
        } catch (SQLException exception) {
            throw new ServletException("Unable to update case.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, CASE_EDIT_ROLES)) {
            return;
        }

        try {
            int caseId = ServletUtil.parseIdFromPath(request.getPathInfo());
            CaseRecord before = caseDAO.getCaseById(caseId);
            if (before == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Case not found.");
                return;
            }

            ApprovalWorkflowUtil.CaseAggregateSnapshot snapshot = new ApprovalWorkflowUtil.CaseAggregateSnapshot();
            snapshot.setCaseRecord(before);
            snapshot.setHearings(hearingDAO.getHearingsByCaseId(caseId));
            snapshot.setDocuments(documentDAO.getDocumentsByCaseId(caseId));
            snapshot.setNotes(lawyerNoteDAO.getNotesByCaseId(caseId));

            caseDAO.deleteCase(caseId);
            User admin = ServletUtil.getSessionUser(request);
            ApprovalRequestRecord requestRecord = new ApprovalRequestRecord();
            requestRecord.setRequestType("ADMIN_CASE_DELETE");
            requestRecord.setRequestedByRole(admin != null ? admin.getRole() : "Admin");
            requestRecord.setRequestedByUser(admin != null ? admin.getId() : 0);
            requestRecord.setRequestedByName(admin != null ? admin.getName() : "Admin");
            requestRecord.setApprovalRole("Judge");
            requestRecord.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_CASE);
            requestRecord.setTargetEntityId(caseId);
            requestRecord.setActionType(ApprovalWorkflowUtil.ACTION_DELETE);
            requestRecord.setRequestTitle("Admin case deletion for " + before.getCaseName());
            requestRecord.setRequestPayload(JsonUtil.getGson().toJson(snapshot));
            requestRecord.setBeforePayload(JsonUtil.getGson().toJson(snapshot));
            requestRecord.setAfterPayload(null);
            requestRecord.setLiveChangeApplied(true);
            requestRecord.setStatus("PENDING");

            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(requestRecord);
            notificationDAO.createForRole(
                    "New admin approval request pending",
                    "A case deletion is waiting for judge review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Judge",
                    null,
                    null,
                    admin != null ? admin.getId() : null
            );
            auditLogDAO.log(admin, "Admin deleted case #" + caseId + " and submitted it for judge review");
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, java.util.Map.of("message", "Case deleted successfully."));
        } catch (SQLException exception) {
            throw new ServletException("Unable to delete case.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    private ApprovalRequestRecord buildApprovalRequest(User admin,
                                                       CaseRecord before,
                                                       CaseRecord after,
                                                       String actionType,
                                                       String title) {
        ApprovalRequestRecord record = new ApprovalRequestRecord();
        record.setRequestType("ADMIN_CASE_" + actionType);
        record.setRequestedByRole(admin != null ? admin.getRole() : "Admin");
        record.setRequestedByUser(admin != null ? admin.getId() : 0);
        record.setRequestedByName(admin != null ? admin.getName() : "Admin");
        record.setApprovalRole("Judge");
        record.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_CASE);
        record.setTargetEntityId(after != null ? after.getCaseId() : (before != null ? before.getCaseId() : null));
        record.setActionType(actionType);
        record.setRequestTitle(title);
        record.setRequestPayload(JsonUtil.getGson().toJson(after != null ? after : before));
        record.setBeforePayload(before == null ? null : JsonUtil.getGson().toJson(before));
        record.setAfterPayload(after == null ? null : JsonUtil.getGson().toJson(after));
        record.setLiveChangeApplied(true);
        record.setStatus("PENDING");
        return record;
    }

    private boolean isValidCase(CaseRecord caseRecord) {
        return caseRecord != null
                && isFilled(caseRecord.getCaseName())
                && isFilled(caseRecord.getClientName())
                && isFilled(caseRecord.getLawyerName())
                && isFilled(caseRecord.getJudgeName())
                && ALLOWED_STATUSES.contains(caseRecord.getStatus())
                && isFilled(caseRecord.getCourtDetails());
    }

    private boolean isFilled(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
