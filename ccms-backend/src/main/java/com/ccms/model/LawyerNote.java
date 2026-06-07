package com.ccms.model;

public class LawyerNote {
    private int noteId;
    private int caseId;
    private Integer hearingId;
    private int lawyerUserId;
    private String lawyerName;
    private String noteType;
    private String content;
    private String approvalStatus;
    private String rejectionReason;
    private String createdAt;
    private String updatedAt;

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public Integer getHearingId() {
        return hearingId;
    }

    public void setHearingId(Integer hearingId) {
        this.hearingId = hearingId;
    }

    public int getLawyerUserId() {
        return lawyerUserId;
    }

    public void setLawyerUserId(int lawyerUserId) {
        this.lawyerUserId = lawyerUserId;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public String getNoteType() {
        return noteType;
    }

    public void setNoteType(String noteType) {
        this.noteType = noteType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
