package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.CaseDAO;
import com.ccms.dao.ChangeRequestDAO;
import com.ccms.dao.DocumentDAO;
import com.ccms.dao.HearingDAO;
import com.ccms.model.CaseDocument;
import com.ccms.model.CaseRecord;
import com.ccms.model.ChangeRequestRecord;
import com.ccms.model.Hearing;
import com.ccms.model.User;
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

@WebServlet("/api/change-requests/*")
public class ChangeRequestServlet extends HttpServlet {
    private final ChangeRequestDAO changeRequestDAO = new ChangeRequestDAO();
    private final CaseDAO caseDAO = new CaseDAO();
    private final HearingDAO hearingDAO = new HearingDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private static final Set<String> REQUEST_ROLES = Set.of("Lawyer", "Judge", "Staff");
    private static final Set<String> JUDGE_ROLES = Set.of("Judge");
    private static final Set<String> ADMIN_ROLES = Set.of("Admin");
    private static final Set<String> ALLOWED_ENTITY_TYPES = Set.of("CASE", "HEARING", "DOCUMENT");
    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of("CREATE", "UPDATE", "DELETE");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            User user = ServletUtil.getSessionUser(request);
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, changeRequestDAO.getRequestsForUser(user));
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch change requests.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, REQUEST_ROLES)) {
            return;
        }

        try {
            User user = ServletUtil.getSessionUser(request);
            ChangeRequestInput input = ServletUtil.readJsonBody(request, ChangeRequestInput.class);
            if (!isValidInput(input)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "A valid entity, action, title, and payload are required.");
                return;
            }

            ChangeRequestRecord requestRecord = new ChangeRequestRecord();
            requestRecord.setEntityType(input.entityType.trim());
            requestRecord.setActionType(input.actionType.trim());
            requestRecord.setEntityId(input.entityId);
            requestRecord.setRequestTitle(input.requestTitle.trim());
            requestRecord.setRequestPayload(input.requestPayload.trim());
            requestRecord.setRequestedByUserId(user.getId());
            requestRecord.setRequestedByName(user.getName());
            requestRecord.setRequestedByRole(user.getRole());
            requestRecord.setStatus(
                    "Lawyer".equals(user.getRole()) ? "PendingJudgeApproval" : "PendingAdminApproval"
            );

            ChangeRequestRecord created = changeRequestDAO.create(requestRecord);
            notificationDAO.create(
                    "Change request submitted",
                    user.getName() + " submitted a " + created.getEntityType().toLowerCase() + " " + created.getActionType().toLowerCase() + " request.",
                    "ChangeRequest",
                    created.getId()
            );

            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, created);
        } catch (SQLException exception) {
            throw new ServletException("Unable to create change request.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        String path = request.getPathInfo();
        if (path == null || !path.endsWith("/review")) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Review endpoint not found.");
            return;
        }

        try {
            int requestId = Integer.parseInt(path.substring(1, path.indexOf("/review")));
            ReviewInput reviewInput = ServletUtil.readJsonBody(request, ReviewInput.class);
            if (reviewInput == null || isBlank(reviewInput.decision)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Review decision is required.");
                return;
            }

            User user = ServletUtil.getSessionUser(request);
            ChangeRequestRecord current = changeRequestDAO.getById(requestId);
            if (current == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Change request not found.");
                return;
            }

            ChangeRequestRecord updated;
            String decision = reviewInput.decision.trim();

            if ("PendingJudgeApproval".equals(current.getStatus())) {
                if (!ServletUtil.requireRole(request, response, JUDGE_ROLES)) {
                    return;
                }

                String nextStatus = "approve".equalsIgnoreCase(decision) ? "PendingAdminApproval" : "Rejected";
                updated = changeRequestDAO.updateJudgeReview(requestId, user, nextStatus, safeTrim(reviewInput.note));
                notificationDAO.create(
                        "Judge reviewed change request",
                        user.getName() + " " + ("Rejected".equals(nextStatus) ? "rejected" : "forwarded") + " request #" + requestId + ".",
                        "ChangeRequestReview",
                        requestId
                );
                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, updated);
                return;
            }

            if ("PendingAdminApproval".equals(current.getStatus())) {
                if (!ServletUtil.requireRole(request, response, ADMIN_ROLES)) {
                    return;
                }

                if ("approve".equalsIgnoreCase(decision)) {
                    applyApprovedRequest(current);
                    updated = changeRequestDAO.updateAdminReview(requestId, user, "Approved", safeTrim(reviewInput.note));
                    notificationDAO.create(
                            "Approved request applied",
                            "Admin applied request #" + requestId + " to the live website.",
                            "AppliedChange",
                            requestId
                    );
                } else {
                    updated = changeRequestDAO.updateAdminReview(requestId, user, "Rejected", safeTrim(reviewInput.note));
                    notificationDAO.create(
                            "Admin rejected change request",
                            "Admin rejected request #" + requestId + ".",
                            "ChangeRequestReview",
                            requestId
                    );
                }

                ServletUtil.sendJson(response, HttpServletResponse.SC_OK, updated);
                return;
            }

            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Only pending requests can be reviewed.");
        } catch (SQLException exception) {
            throw new ServletException("Unable to review change request.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid change request id.");
        }
    }

    private void applyApprovedRequest(ChangeRequestRecord requestRecord) throws SQLException {
        switch (requestRecord.getEntityType()) {
            case "CASE":
                applyCaseRequest(requestRecord);
                return;
            case "HEARING":
                applyHearingRequest(requestRecord);
                return;
            case "DOCUMENT":
                applyDocumentRequest(requestRecord);
                return;
            default:
                throw new SQLException("Unsupported request entity type.");
        }
    }

    private void applyCaseRequest(ChangeRequestRecord requestRecord) throws SQLException {
        if ("DELETE".equals(requestRecord.getActionType())) {
            if (requestRecord.getEntityId() != null) {
                caseDAO.deleteCase(requestRecord.getEntityId());
            }
            return;
        }

        CaseRecord caseRecord = JsonUtil.getGson().fromJson(requestRecord.getRequestPayload(), CaseRecord.class);
        if ("CREATE".equals(requestRecord.getActionType())) {
            caseDAO.createCase(caseRecord);
            return;
        }

        if (requestRecord.getEntityId() != null) {
            caseDAO.updateCase(requestRecord.getEntityId(), caseRecord);
        }
    }

    private void applyHearingRequest(ChangeRequestRecord requestRecord) throws SQLException {
        Hearing hearing = JsonUtil.getGson().fromJson(requestRecord.getRequestPayload(), Hearing.class);
        if (hearing != null && hearing.getCaseId() > 0) {
            if (hearing.getHearingDate() != null) {
                hearing.setHearingDate(hearing.getHearingDate().replace("T", " "));
            }
            hearingDAO.addHearing(hearing);
        }
    }

    private void applyDocumentRequest(ChangeRequestRecord requestRecord) throws SQLException {
        CaseDocument document = JsonUtil.getGson().fromJson(requestRecord.getRequestPayload(), CaseDocument.class);
        if (document != null && document.getCaseId() > 0) {
            documentDAO.addDocument(document);
        }
    }

    private boolean isValidInput(ChangeRequestInput input) {
        return input != null
                && !isBlank(input.entityType)
                && !isBlank(input.actionType)
                && !isBlank(input.requestTitle)
                && !isBlank(input.requestPayload)
                && ALLOWED_ENTITY_TYPES.contains(input.entityType.trim())
                && ALLOWED_ACTION_TYPES.contains(input.actionType.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private static class ChangeRequestInput {
        private String entityType;
        private String actionType;
        private Integer entityId;
        private String requestTitle;
        private String requestPayload;
    }

    private static class ReviewInput {
        private String decision;
        private String note;
    }
}
