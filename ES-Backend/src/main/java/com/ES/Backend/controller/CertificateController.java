package com.ES.Backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import com.ES.Backend.service.CertificateService;
import com.ES.Backend.service.JwtService;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {
    private final CertificateService service;
    private final JwtService jwtService;

    public CertificateController(CertificateService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    record UploadResponse(String id) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadCertificate(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @RequestPart("file") MultipartFile file,
        @RequestPart("password") String password
    ) throws Exception {
        String token = authHeader.substring(7);
        String user = jwtService.extractUser(token);
        String id = service.encrypt(user, file, password.toCharArray());
        return new UploadResponse(id);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<byte[]> downloadCertificate(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String id,
        @RequestParam String password
    ) throws Exception {
        // Validate token
        String token = authHeader.substring(7);
        jwtService.extractUser(token);

        byte[] data = service.decrypt(id, password.toCharArray());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate.p12")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }
}