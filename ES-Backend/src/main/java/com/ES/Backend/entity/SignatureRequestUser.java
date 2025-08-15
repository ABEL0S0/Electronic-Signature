package com.ES.Backend.entity;
import jakarta.persistence.Transient;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

@Entity
public class SignatureRequestUser {

    @Transient
    private Long signatureRequestId;

    public Long getSignatureRequestId() {
        return signatureRequestId;
    }

    public void setSignatureRequestId(Long signatureRequestId) {
        this.signatureRequestId = signatureRequestId;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int page;
    private int posX;
    private int posY;

    private String status = "PENDIENTE";

    private String certificateId;
    private String certificatePassword;

    @ManyToOne
    @JoinColumn(name = "signature_request_id")
    @JsonIgnore
    private SignatureRequest signatureRequest;

    private LocalDateTime respondedAt;

    public SignatureRequestUser() {
    }

    public SignatureRequestUser(Long id, Long userId, int page, int posX, int posY, String status, String certificateId,
            String certificatePassword, SignatureRequest signatureRequest, LocalDateTime respondedAt) {
        this.id = id;
        this.userId = userId;
        this.page = page;
        this.posX = posX;
        this.posY = posY;
        this.status = status;
        this.certificateId = certificateId;
        this.certificatePassword = certificatePassword;
        this.signatureRequest = signatureRequest;
        this.respondedAt = respondedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public SignatureRequest getSignatureRequest() {
        return signatureRequest;
    }

    public void setSignatureRequest(SignatureRequest signatureRequest) {
        this.signatureRequest = signatureRequest;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "PENDIENTE";
    }
}