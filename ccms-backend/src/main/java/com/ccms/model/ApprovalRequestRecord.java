package com.ccms.model;

public class ApprovalRequestRecord {
    private int id;
    private String requestType;
    private String requestedByRole;
    private int requestedByUser;
    private String requestedByName;
    private String approvalRole;
    private String targetEntityType;
    private Integer targetEntityId;
    private String actionType;
    private String status;
    private String rejectionReason;
    private String requestTitle;
    private String requestPayload;
    private String beforePayload;
    private String afterPayload;
    private boolean liveChangeApplied;
    private Integer reviewedByUserId;
    private String reviewedByUserName;
    private String reviewedByRole;
    private String reviewNote;
    private String createdAt;
    private String reviewedAt;
    private String updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestedByRole() {
        return requestedByRole;
    }

    public void setRequestedByRole(String requestedByRole) {
        this.requestedByRole = requestedByRole;
    }

    public int getRequestedByUser() {
        return requestedByUser;
    }

    public void setRequestedByUser(int requestedByUser) {
        this.requestedByUser = requestedByUser;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getApprovalRole() {
        return approvalRole;
    }

    public void setApprovalRole(String approvalRole) {
        this.approvalRole = approvalRole;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetEntityType(String targetEntityType) {
        this.targetEntityType = targetEntityType;
    }

    public Integer getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(Integer targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRequestTitle() {
        return requestTitle;
    }

    public void setRequestTitle(String requestTitle) {
        this.requestTitle = requestTitle;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getBeforePayload() {
        return beforePayload;
    }

    public void setBeforePayload(String beforePayload) {
        this.beforePayload = beforePayload;
    }

    public String getAfterPayload() {
        return afterPayload;
    }

    public void setAfterPayload(String afterPayload) {
        this.afterPayload = afterPayload;
    }

    public boolean isLiveChangeApplied() {
        return liveChangeApplied;
    }

    public void setLiveChangeApplied(boolean liveChangeApplied) {
        this.liveChangeApplied = liveChangeApplied;
    }

    public Integer getReviewedByUserId() {
        return reviewedByUserId;
    }

    public void setReviewedByUserId(Integer reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
    }

    public String getReviewedByUserName() {
        return reviewedByUserName;
    }

    public void setReviewedByUserName(String reviewedByUserName) {
        this.reviewedByUserName = reviewedByUserName;
    }

    public String getReviewedByRole() {
        return reviewedByRole;
    }

    public void setReviewedByRole(String reviewedByRole) {
        this.reviewedByRole = reviewedByRole;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
