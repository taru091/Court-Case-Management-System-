package com.ccms.servlet;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.LawyerNoteDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.LawyerNote;
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

@WebServlet("/api/lawyer-notes/*")
public class LawyerNoteServlet extends HttpServlet {
    private static final Set<String> LAWYER_ROLES = Set.of("Lawyer");
    private static final Set<String> ALLOWED_NOTE_TYPES = Set.of(
            "PERSONAL_HEARING_NOTE",
            "ADVOCATE_COMMENT",
            "PREPARATION_NOTE"
    );

    private final LawyerNoteDAO lawyerNoteDAO = new LawyerNoteDAO();
    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, LAWYER_ROLES)) {
            return;
        }

        try {
            User lawyer = ServletUtil.getSessionUser(request);
            String caseIdValue = request.getParameter("caseId");
            Integer caseId = isBlank(caseIdValue) ? null : Integer.parseInt(caseIdValue);
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, lawyerNoteDAO.getNotesForLawyer(lawyer.getId(), caseId));
        } catch (SQLException exception) {
            throw new ServletException("Unable to fetch lawyer notes.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid case id.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, LAWYER_ROLES)) {
            return;
        }

        try {
            User lawyer = ServletUtil.getSessionUser(request);
            LawyerNote note = ServletUtil.readJsonBody(request, LawyerNote.class);
            if (!isValidNote(note)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Case, note type, and content are required.");
                return;
            }

            note.setLawyerUserId(lawyer.getId());
            note.setLawyerName(lawyer.getName());
            note.setApprovalStatus("PENDING");
            note.setRejectionReason(null);

            LawyerNote createdNote = lawyerNoteDAO.create(note);
            ApprovalRequestRecord requestRecord = buildApprovalRequest(lawyer, null, createdNote, ApprovalWorkflowUtil.ACTION_CREATE);
            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(requestRecord);

            notificationDAO.createForRole(
                    "New lawyer approval request pending",
                    "A lawyer note is awaiting admin approval.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Admin",
                    null,
                    null,
                    lawyer.getId()
            );
            notificationDAO.createForUser(
                    "Your note is awaiting admin approval",
                    "Your " + readableNoteType(createdNote.getNoteType()) + " has been submitted for review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    lawyer,
                    lawyer.getId()
            );
            auditLogDAO.log(lawyer, "Lawyer submitted " + readableNoteType(createdNote.getNoteType()) + " for admin approval");
            ServletUtil.sendJson(response, HttpServletResponse.SC_CREATED, createdNote);
        } catch (SQLException exception) {
            throw new ServletException("Unable to create lawyer note.", exception);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!ServletUtil.requireRole(request, response, LAWYER_ROLES)) {
            return;
        }

        try {
            int noteId = ServletUtil.parseIdFromPath(request.getPathInfo());
            User lawyer = ServletUtil.getSessionUser(request);
            LawyerNote before = lawyerNoteDAO.getById(noteId);
            if (before == null || before.getLawyerUserId() != lawyer.getId()) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_NOT_FOUND, "Lawyer note not found.");
                return;
            }

            LawyerNote input = ServletUtil.readJsonBody(request, LawyerNote.class);
            if (!isValidNote(input)) {
                ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Case, note type, and content are required.");
                return;
            }

            LawyerNote after = new LawyerNote();
            after.setNoteId(before.getNoteId());
            after.setCaseId(input.getCaseId());
            after.setHearingId(input.getHearingId());
            after.setLawyerUserId(lawyer.getId());
            after.setLawyerName(lawyer.getName());
            after.setNoteType(input.getNoteType());
            after.setContent(input.getContent());
            after.setApprovalStatus("PENDING");
            after.setRejectionReason(null);

            lawyerNoteDAO.update(noteId, after);
            LawyerNote updatedNote = lawyerNoteDAO.getById(noteId);
            ApprovalRequestRecord requestRecord = buildApprovalRequest(lawyer, before, updatedNote, ApprovalWorkflowUtil.ACTION_UPDATE);
            ApprovalRequestRecord createdRequest = approvalRequestDAO.create(requestRecord);

            notificationDAO.createForRole(
                    "New lawyer approval request pending",
                    "A lawyer note update is awaiting admin approval.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    "Admin",
                    null,
                    null,
                    lawyer.getId()
            );
            notificationDAO.createForUser(
                    "Your note is awaiting admin approval",
                    "Your " + readableNoteType(updatedNote.getNoteType()) + " update has been submitted for review.",
                    "ApprovalWorkflow",
                    createdRequest.getId(),
                    lawyer,
                    lawyer.getId()
            );
            auditLogDAO.log(lawyer, "Lawyer updated " + readableNoteType(updatedNote.getNoteType()) + " and submitted it for admin approval");
            ServletUtil.sendJson(response, HttpServletResponse.SC_OK, updatedNote);
        } catch (SQLException exception) {
            throw new ServletException("Unable to update lawyer note.", exception);
        } catch (NumberFormatException exception) {
            ServletUtil.sendMessage(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid lawyer note id.");
        }
    }

    private ApprovalRequestRecord buildApprovalRequest(User lawyer, LawyerNote before, LawyerNote after, String actionType) {
        ApprovalRequestRecord record = new ApprovalRequestRecord();
        record.setRequestType("LAWYER_NOTE_" + actionType);
        record.setRequestedByRole(lawyer.getRole());
        record.setRequestedByUser(lawyer.getId());
        record.setRequestedByName(lawyer.getName());
        record.setApprovalRole("Admin");
        record.setTargetEntityType(ApprovalWorkflowUtil.ENTITY_LAWYER_NOTE);
        record.setTargetEntityId(after.getNoteId());
        record.setActionType(actionType);
        record.setRequestTitle("Lawyer note review for case #" + after.getCaseId());
        record.setRequestPayload(JsonUtil.getGson().toJson(after));
        record.setBeforePayload(before == null ? null : JsonUtil.getGson().toJson(before));
        record.setAfterPayload(JsonUtil.getGson().toJson(after));
        record.setLiveChangeApplied(false);
        record.setStatus("PENDING");
        return record;
    }

    private boolean isValidNote(LawyerNote note) {
        return note != null
                && note.getCaseId() > 0
                && !isBlank(note.getContent())
                && ALLOWED_NOTE_TYPES.contains(note.getNoteType());
    }

    private String readableNoteType(String noteType) {
        if ("PERSONAL_HEARING_NOTE".equals(noteType)) {
            return "personal hearing note";
        }
        if ("ADVOCATE_COMMENT".equals(noteType)) {
            return "advocate comment";
        }
        return "preparation note";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
