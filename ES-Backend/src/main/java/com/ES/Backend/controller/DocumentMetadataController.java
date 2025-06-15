package com.ES.Backend.controller;

import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.service.DocumentMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentMetadataController {

    private final DocumentMetadataService service;

    public DocumentMetadataController(DocumentMetadataService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam("userUuid") String userUuid,
        @RequestParam("signature") String signature,
        @RequestParam("hashAlgorithm") String hashAlgorithm
    ) {
        try {
            DocumentMetadata saved = service.saveDocument(file, userUuid, signature, hashAlgorithm);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}

