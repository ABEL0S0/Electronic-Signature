package com.ES.Backend.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;

@Entity
public class SignatureRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String documentPath;

    private String status = "PENDIENTE";

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "signatureRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SignatureRequestUser> users;

    public SignatureRequest() {
    }

    public SignatureRequest(Long id, String documentPath, String status, LocalDateTime createdAt,
            List<SignatureRequestUser> users) {
        this.id = id;
        this.documentPath = documentPath;
        this.status = status;
        this.createdAt = createdAt;
        this.users = users;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDIENTE";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SignatureRequestUser> getUsers() {
        return users;
    }

    public void setUsers(List<SignatureRequestUser> users) {
        this.users = users;
    }
    
}
