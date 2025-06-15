package com.ES.Backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "document_metadata")
public class DocumentMetadata {

    @Id
    private String id;
    private String userUuid;
    private String fileName;
    private String filePath;
    private String signature;
    private String hashAlgorithm;
    private LocalDateTime uploadedAt;

    public DocumentMetadata() {
    }

    public DocumentMetadata(String userUuid, String fileName, String filePath, String signature, String hashAlgorithm) {
        this.userUuid = userUuid;
        this.fileName = fileName;
        this.filePath = filePath;
        this.signature = signature;
        this.hashAlgorithm = hashAlgorithm;
        this.uploadedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

}

