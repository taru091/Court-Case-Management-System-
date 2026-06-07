package com.ccms.model;

public class ChangeRequestRecord {
    private int id;
    private String entityType;
    private String actionType;
    private Integer entityId;
    private String requestTitle;
    private String requestPayload;
    private String status;
    private int requestedByUserId;
    private String requestedByName;
    private String requestedByRole;
    private Integer judgeReviewerId;
    private String judgeReviewerName;
    private Integer adminReviewerId;
    private String adminReviewerName;
    private String reviewNote;
    private String createdAt;
    private String updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(int requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getRequestedByRole() {
        return requestedByRole;
    }

    public void setRequestedByRole(String requestedByRole) {
        this.requestedByRole = requestedByRole;
    }

    public Integer getJudgeReviewerId() {
        return judgeReviewerId;
    }

    public void setJudgeReviewerId(Integer judgeReviewerId) {
        this.judgeReviewerId = judgeReviewerId;
    }

    public String getJudgeReviewerName() {
        return judgeReviewerName;
    }

    public void setJudgeReviewerName(String judgeReviewerName) {
        this.judgeReviewerName = judgeReviewerName;
    }

    public Integer getAdminReviewerId() {
        return adminReviewerId;
    }

    public void setAdminReviewerId(Integer adminReviewerId) {
        this.adminReviewerId = adminReviewerId;
    }

    public String getAdminReviewerName() {
        return adminReviewerName;
    }

    public void setAdminReviewerName(String adminReviewerName) {
        this.adminReviewerName = adminReviewerName;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
