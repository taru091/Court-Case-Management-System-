package com.ccms.util;

import com.ccms.dao.AdminNotificationDAO;
import com.ccms.dao.ApprovalRequestDAO;
import com.ccms.dao.AuditLogDAO;
import com.ccms.dao.CaseDAO;
import com.ccms.dao.DocumentDAO;
import com.ccms.dao.HearingDAO;
import com.ccms.dao.LawyerNoteDAO;
import com.ccms.dao.SiteSettingDAO;
import com.ccms.dao.UserDAO;
import com.ccms.model.ApprovalRequestRecord;
import com.ccms.model.CaseDocument;
import com.ccms.model.CaseRecord;
import com.ccms.model.Hearing;
import com.ccms.model.LawyerNote;
import com.ccms.model.SiteSettingRecord;
import com.ccms.model.User;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

public class ApprovalWorkflowUtil {
    public static final String ENTITY_CASE = "CASE";
    public static final String ENTITY_HEARING = "HEARING";
    public static final String ENTITY_DOCUMENT = "DOCUMENT";
    public static final String ENTITY_LAWYER_NOTE = "LAWYER_NOTE";
    public static final String ENTITY_SITE_SETTING = "SITE_SETTING";
    public static final String ENTITY_NOTIFICATION = "NOTIFICATION";
    public static final String ENTITY_USER = "USER";

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    private final ApprovalRequestDAO approvalRequestDAO = new ApprovalRequestDAO();
    private final AdminNotificationDAO notificationDAO = new AdminNotificationDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final CaseDAO caseDAO = new CaseDAO();
    private final HearingDAO hearingDAO = new HearingDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final LawyerNoteDAO lawyerNoteDAO = new LawyerNoteDAO();
    private final SiteSettingDAO siteSettingDAO = new SiteSettingDAO();
    private final UserDAO userDAO = new UserDAO();

    public ApprovalRequestRecord reviewRequest(ApprovalRequestRecord current,
                                               User reviewer,
                                               String decision,
                                               String reviewNote,
                                               String rejectionReason) throws SQLException {
        boolean approved = "approve".equalsIgnoreCase(decision);

        if (approved) {
            if (!current.isLiveChangeApplied()) {
                applyPendingChange(current);
            }
            ApprovalRequestRecord updated = approvalRequestDAO.updateReview(
                    current.getId(),
                    reviewer,
                    "APPROVED",
                    null,
                    safeTrim(reviewNote)
            );
            notifyRequester(current, reviewer, true, null);
            auditLogDAO.log(reviewer, reviewer.getRole() + " approved request #" + current.getId() + " (" + current.getRequestTitle() + ")");
            return updated;
        }

        if (current.isLiveChangeApplied()) {
            revertLiveChange(current);
        } else {
            rejectPendingChange(current, rejectionReason);
        }

        ApprovalRequestRecord updated = approvalRequestDAO.updateReview(
                current.getId(),
                reviewer,
                "REJECTED",
                safeTrim(rejectionReason),
                safeTrim(reviewNote)
        );
        notifyRequester(current, reviewer, false, rejectionReason);
        auditLogDAO.log(reviewer, reviewer.getRole() + " rejected request #" + current.getId() + " (" + current.getRequestTitle() + ")");
        return updated;
    }

    private void applyPendingChange(ApprovalRequestRecord current) throws SQLException {
        switch (current.getTargetEntityType()) {
            case ENTITY_DOCUMENT:
                if (current.getTargetEntityId() != null) {
                    documentDAO.updateApprovalStatus(current.getTargetEntityId(), "APPROVED", null, true);
                }
                return;
            case ENTITY_LAWYER_NOTE:
                if (current.getTargetEntityId() != null) {
                    lawyerNoteDAO.updateApprovalStatus(current.getTargetEntityId(), "APPROVED", null);
                }
                return;
            default:
                // Live entity changes are already applied by admin actions.
        }
    }

    private void rejectPendingChange(ApprovalRequestRecord current, String rejectionReason) throws SQLException {
        switch (current.getTargetEntityType()) {
            case ENTITY_DOCUMENT:
                if (current.getTargetEntityId() != null) {
                    documentDAO.updateApprovalStatus(current.getTargetEntityId(), "REJECTED", rejectionReason, false);
                }
                return;
            case ENTITY_LAWYER_NOTE:
                if (current.getTargetEntityId() != null) {
                    lawyerNoteDAO.updateApprovalStatus(current.getTargetEntityId(), "REJECTED", rejectionReason);
                }
                return;
            default:
                // No pending entity state to mutate.
        }
    }

    private void revertLiveChange(ApprovalRequestRecord current) throws SQLException {
        switch (current.getTargetEntityType()) {
            case ENTITY_CASE:
                revertCase(current);
                return;
            case ENTITY_HEARING:
                revertHearing(current);
                return;
            case ENTITY_DOCUMENT:
                revertDocument(current);
                return;
            case ENTITY_SITE_SETTING:
                revertSiteSetting(current);
                return;
            case ENTITY_NOTIFICATION:
                revertNotification(current);
                return;
            case ENTITY_USER:
                revertUser(current);
                return;
            default:
                throw new SQLException("Unsupported approval entity type: " + current.getTargetEntityType());
        }
    }

    private void revertCase(ApprovalRequestRecord current) throws SQLException {
        if (ACTION_CREATE.equals(current.getActionType())) {
            if (current.getTargetEntityId() != null) {
                caseDAO.deleteCase(current.getTargetEntityId());
            }
            return;
        }

        if (ACTION_UPDATE.equals(current.getActionType())) {
            CaseRecord before = JsonUtil.getGson().fromJson(current.getBeforePayload(), CaseRecord.class);
            if (before != null) {
                caseDAO.updateCase(before.getCaseId(), before);
            }
            return;
        }

        if (ACTION_DELETE.equals(current.getActionType())) {
            CaseAggregateSnapshot snapshot = JsonUtil.getGson().fromJson(current.getBeforePayload(), CaseAggregateSnapshot.class);
            if (snapshot != null && snapshot.caseRecord != null) {
                caseDAO.restoreCase(snapshot.caseRecord);
                for (Hearing hearing : safeList(snapshot.hearings)) {
                    hearingDAO.restoreHearing(hearing);
                }
                for (CaseDocument document : safeList(snapshot.documents)) {
                    documentDAO.restoreDocument(document);
                }
                for (LawyerNote note : safeList(snapshot.notes)) {
                    lawyerNoteDAO.restore(note);
                }
            }
        }
    }

    private void revertHearing(ApprovalRequestRecord current) throws SQLException {
        if (ACTION_CREATE.equals(current.getActionType())) {
            if (current.getTargetEntityId() != null) {
                hearingDAO.deleteHearing(current.getTargetEntityId());
            }
            return;
        }

        Hearing before = JsonUtil.getGson().fromJson(current.getBeforePayload(), Hearing.class);
        if (before == null) {
            return;
        }

        if (ACTION_UPDATE.equals(current.getActionType())) {
            hearingDAO.updateHearing(before.getHearingId(), before);
            return;
        }

        if (ACTION_DELETE.equals(current.getActionType())) {
            hearingDAO.restoreHearing(before);
        }
    }

    private void revertDocument(ApprovalRequestRecord current) throws SQLException {
        if (ACTION_CREATE.equals(current.getActionType())) {
            if (current.getTargetEntityId() != null) {
                documentDAO.deleteDocument(current.getTargetEntityId());
            }
            return;
        }

        CaseDocument before = JsonUtil.getGson().fromJson(current.getBeforePayload(), CaseDocument.class);
        if (before == null) {
            return;
        }

        if (ACTION_DELETE.equals(current.getActionType())) {
            documentDAO.restoreDocument(before);
            return;
        }

        documentDAO.restoreDocument(before);
    }

    private void revertSiteSetting(ApprovalRequestRecord current) throws SQLException {
        SiteSettingRecord before = JsonUtil.getGson().fromJson(current.getBeforePayload(), SiteSettingRecord.class);
        if (before == null) {
            SiteSettingRecord after = JsonUtil.getGson().fromJson(current.getAfterPayload(), SiteSettingRecord.class);
            if (after != null) {
                siteSettingDAO.delete(after.getKey());
            }
            return;
        }

        siteSettingDAO.upsert(before.getKey(), before.getValue(), before.getUpdatedByUserId());
    }

    private void revertNotification(ApprovalRequestRecord current) throws SQLException {
        if (current.getTargetEntityId() != null) {
            notificationDAO.delete(current.getTargetEntityId());
        }
    }

    private void revertUser(ApprovalRequestRecord current) throws SQLException {
        User before = JsonUtil.getGson().fromJson(current.getBeforePayload(), User.class);
        if (before != null) {
            userDAO.updateManagedUser(before);
        }
    }

    private void notifyRequester(ApprovalRequestRecord current,
                                 User reviewer,
                                 boolean approved,
                                 String rejectionReason) throws SQLException {
        String title = approved ? "Approval completed" : "Approval rejected";
        String message = approved
                ? reviewer.getRole() + " approved " + current.getRequestTitle() + "."
                : reviewer.getRole() + " rejected " + current.getRequestTitle() +
                (rejectionReason == null || rejectionReason.trim().isEmpty() ? "." : ": " + rejectionReason.trim());

        User requester = userDAO.findById(current.getRequestedByUser());
        if (requester != null) {
            notificationDAO.createForUser(title, message, "ApprovalWorkflow", current.getId(), requester, reviewer.getId());
            return;
        }

        notificationDAO.createForRole(title, message, "ApprovalWorkflow", current.getId(), current.getRequestedByRole(), current.getRequestedByUser(), current.getRequestedByName(), reviewer.getId());
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    public static final class CaseAggregateSnapshot {
        private CaseRecord caseRecord;
        private List<Hearing> hearings;
        private List<CaseDocument> documents;
        private List<LawyerNote> notes;

        public CaseRecord getCaseRecord() {
            return caseRecord;
        }

        public void setCaseRecord(CaseRecord caseRecord) {
            this.caseRecord = caseRecord;
        }

        public List<Hearing> getHearings() {
            return hearings;
        }

        public void setHearings(List<Hearing> hearings) {
            this.hearings = hearings;
        }

        public List<CaseDocument> getDocuments() {
            return documents;
        }

        public void setDocuments(List<CaseDocument> documents) {
            this.documents = documents;
        }

        public List<LawyerNote> getNotes() {
            return notes;
        }

        public void setNotes(List<LawyerNote> notes) {
            this.notes = notes;
        }
    }
}
