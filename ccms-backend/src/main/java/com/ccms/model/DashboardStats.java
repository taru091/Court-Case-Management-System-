package com.ccms.model;

public class DashboardStats {
    private int totalCases;
    private int activeCases;
    private int pendingCases;
    private int closedCases;
    private int upcomingHearings;

    public int getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(int totalCases) {
        this.totalCases = totalCases;
    }

    public int getActiveCases() {
        return activeCases;
    }

    public void setActiveCases(int activeCases) {
        this.activeCases = activeCases;
    }

    public int getPendingCases() {
        return pendingCases;
    }

    public void setPendingCases(int pendingCases) {
        this.pendingCases = pendingCases;
    }

    public int getClosedCases() {
        return closedCases;
    }

    public void setClosedCases(int closedCases) {
        this.closedCases = closedCases;
    }

    public int getUpcomingHearings() {
        return upcomingHearings;
    }

    public void setUpcomingHearings(int upcomingHearings) {
        this.upcomingHearings = upcomingHearings;
    }
}
