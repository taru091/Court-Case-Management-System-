package com.ccms.model;

public class JudgeWorkload {
    private String judgeName;
    private int totalCases;

    public JudgeWorkload() {
    }

    public JudgeWorkload(String judgeName, int totalCases) {
        this.judgeName = judgeName;
        this.totalCases = totalCases;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
    }

    public int getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(int totalCases) {
        this.totalCases = totalCases;
    }
}
