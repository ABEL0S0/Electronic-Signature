package com.ES.Backend.entity;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "certificate_requests")
public class CertificateRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    
    @Column(name = "user_name", nullable = false)
    private String userName;
    
    @Column(name = "organization", nullable = false)
    private String organization;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    
    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;
    
    @Column(name = "processed_at")
    private Date processedAt;
    
    @Column(name = "processed_by")
    private String processedBy;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Constructors
    public CertificateRequest() {}

    public CertificateRequest(String userEmail, String userName, String organization, String password) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.organization = organization;
        this.password = password;
        this.requestedAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
} 