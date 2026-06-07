package com.ccms.servlet;

import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.User;
import com.ccms.util.ApprovalWorkflowUtil;
import com.ccms.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/approval-requests/*")
public class ApprovalRequestServlet extends HttpServlet {
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final ApprovalWorkflowUtil workflowUtil = new ApprovalWorkflowUtil();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireLogin(request, response)) {
            return;
        }

        try {
            User user = ServletUtil.getSessionUser(request);
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, approvalRequestDAO.getRequestsForUser(user));
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch approval requests.", exception);
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
            ReviewInput input = ServletUtil.readJsonBody(request, ReviewInput.class);
            if (input == null || isBlank(input.decision)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Review decision is required.");
                return;
            }

            ApprovalRequestRecord current = approvalRequestDAO.getById(requestId);
            if (current == null) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Approval request not found.");
                return;
            }

            if (!"PENDING".equalsIgnoreCase(current.getStatus())) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Only pending approval requests can be reviewed.");
                return;
            }

            User reviewer = ServletUtil.getSessionUser(request);
            if (reviewer == null || !current.getApprovalRole().equalsIgnoreCase(reviewer.getRole())) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to review this request.");
                return;
            }

            boolean approved = "approve".equalsIgnoreCase(input.decision);
            if (!approved && isBlank(input.rejectionReason)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Rejection reason is required.");
                return;
            }

            ApprovalRequestRecord updated = workflowUtil.reviewRequest(
                    current,
                    reviewer,
                    input.decision,
                    input.note,
                    input.rejectionReason
            );

            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, updated);
        } catch (SQLException exception) {
            throw new ServletException("Unable to review approval request.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid approval request id.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ReviewInput {
        private String decision;
        private String note;
        private String rejectionReason;
    }
}
