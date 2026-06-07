package com.ccms.model;

public class User {
    private int id;
    private String username;
    private String name;
    private String mobile;
    private String email;
    private String password;
    private String role;
    private String occupation;
    private String barCouncilNumber;
    private String courtId;
    private String aadhaarNumber;
    private String profilePhotoUrl;
    private String approvalStatus;
    private String availabilityStatus;

    public User() {
    }

    public User(int id,
                String username,
                String name,
                String mobile,
                String email,
                String password,
                String role,
                String occupation,
                String barCouncilNumber,
                String courtId,
                String aadhaarNumber,
                String profilePhotoUrl,
                String approvalStatus,
                String availabilityStatus) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.password = password;
        this.role = role;
        this.occupation = occupation;
        this.barCouncilNumber = barCouncilNumber;
        this.courtId = courtId;
        this.aadhaarNumber = aadhaarNumber;
        this.profilePhotoUrl = profilePhotoUrl;
        this.approvalStatus = approvalStatus;
        this.availabilityStatus = availabilityStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getBarCouncilNumber() {
        return barCouncilNumber;
    }

    public void setBarCouncilNumber(String barCouncilNumber) {
        this.barCouncilNumber = barCouncilNumber;
    }

    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
