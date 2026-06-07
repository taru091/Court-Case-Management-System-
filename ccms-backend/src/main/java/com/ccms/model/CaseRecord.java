package com.ccms.model;

public class CaseRecord {
    private int caseId;
    private String caseName;
    private String clientName;
    private String lawyerName;
    private Integer lawyerUserId;
    private String judgeName;
    private Integer judgeUserId;
    private String status;
    private String courtDetails;
    private String createdAt;
    private String nextHearingDate;
    private String nextCourtroom;

    public CaseRecord() {
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public Integer getLawyerUserId() {
        return lawyerUserId;
    }

    public void setLawyerUserId(Integer lawyerUserId) {
        this.lawyerUserId = lawyerUserId;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
    }

    public Integer getJudgeUserId() {
        return judgeUserId;
    }

    public void setJudgeUserId(Integer judgeUserId) {
        this.judgeUserId = judgeUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCourtDetails() {
        return courtDetails;
    }

    public void setCourtDetails(String courtDetails) {
        this.courtDetails = courtDetails;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getNextHearingDate() {
        return nextHearingDate;
    }

    public void setNextHearingDate(String nextHearingDate) {
        this.nextHearingDate = nextHearingDate;
    }

    public String getNextCourtroom() {
        return nextCourtroom;
    }

    public void setNextCourtroom(String nextCourtroom) {
        this.nextCourtroom = nextCourtroom;
    }
}
