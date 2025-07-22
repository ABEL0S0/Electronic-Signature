package com.ES.Backend.controller;

import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.service.DocumentMetadataService;
import com.ES.Backend.service.JwtService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentMetadataController {

    private final DocumentMetadataService service;
    private final JwtService jwtService;

    public DocumentMetadataController(DocumentMetadataService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @RequestParam("file") MultipartFile file
    ) {
        try {
            String token = authHeader.substring(7);
            String user = jwtService.extractUser(token);
            DocumentMetadata saved = service.saveDocument(file, user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String id
    ) throws MalformedURLException {
        String token = authHeader.substring(7);
        String user = jwtService.extractUser(token);

        DocumentMetadata metadata = service.downloadDocument(id);

        if (!metadata.getuser().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path path = Paths.get(metadata.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
            .body(resource);
    }

    @GetMapping("/list")
    public ResponseEntity<List<DocumentMetadata>> getDocumentsByUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        String token = authHeader.substring(7);
        String user = jwtService.extractUser(token);
        List<DocumentMetadata> documents = service.getDocumentsByUser(user);
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/user/{user}")
    public void deleteDocumentsByUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String user
    ) {
        String token = authHeader.substring(7);
        jwtService.extractUser(token);
        service.deleteDocumentsByUser(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String id
    ) {
        String token = authHeader.substring(7);
        String user = jwtService.extractUser(token);
        DocumentMetadata metadata = service.downloadDocument(id); 

        if (!metadata.getuser().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            service.deleteDocument(id);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error deleting document: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewDocument(@PathVariable String id) {
        Resource pdf = service.loadPdfById(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.getFilename() + "\"")
            .body(pdf);
    }
}