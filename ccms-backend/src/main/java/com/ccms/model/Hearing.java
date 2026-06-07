package com.ccms.model;

public class Hearing {
    private int hearingId;
    private int caseId;
    private String caseName;
    private String hearingDate;
    private String courtroom;
    private Integer judgeUserId;
    private String judgeName;

    public Hearing() {
    }

    public int getHearingId() {
        return hearingId;
    }

    public void setHearingId(int hearingId) {
        this.hearingId = hearingId;
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

    public String getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(String hearingDate) {
        this.hearingDate = hearingDate;
    }

    public String getCourtroom() {
        return courtroom;
    }

    public void setCourtroom(String courtroom) {
        this.courtroom = courtroom;
    }

    public Integer getJudgeUserId() {
        return judgeUserId;
    }

    public void setJudgeUserId(Integer judgeUserId) {
        this.judgeUserId = judgeUserId;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
    }
}
