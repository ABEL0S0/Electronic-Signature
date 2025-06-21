package com.ES.Backend.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "certificates")
public class Certificate {
    @Id
    private String id;
    private String userUUID;
    private String filename;
    private byte[] data;        // certificado cifrado
    private String saltHex;     // salt PBKDF2
    private String ivHex;       // IV AES
    private Instant createdAt = Instant.now();
    
    public Certificate() {
    }

    public Certificate(String id, String userUUID, String filename, byte[] data, String saltHex, String ivHex,
            Instant createdAt) {
        this.id = id;
        this.userUUID = userUUID;
        this.filename = filename;
        this.data = data;
        this.saltHex = saltHex;
        this.ivHex = ivHex;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getSaltHex() {
        return saltHex;
    }

    public void setSaltHex(String saltHex) {
        this.saltHex = saltHex;
    }

    public String getIvHex() {
        return ivHex;
    }

    public void setIvHex(String ivHex) {
        this.ivHex = ivHex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
}
