package com.ES.Backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "document_metadata")
public class DocumentMetadata {

    @Id
    private String id;
    private String user;
    private String fileName;
    private String filePath;
    private boolean isSigned = false;
    private LocalDateTime uploadedAt;

    public DocumentMetadata() {
    }

    public DocumentMetadata(String user, String fileName, String filePath, boolean isSigned ) {
        this.user = user;
        this.fileName = fileName;
        this.filePath = filePath;
        this.isSigned = isSigned;
        this.uploadedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getuser() {
        return user;
    }

    public void setuser(String user) {
        this.user = user;
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

    public boolean isSigned() {
        return isSigned;
    }

    public void setSigned(boolean isSigned) {
        this.isSigned = isSigned;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

}

