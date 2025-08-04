package com.ES.Backend.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ES.Backend.data.CertRequestDTO;
import com.ES.Backend.entity.CertificateRequest;
import com.ES.Backend.service.CertificateRequestService;
import com.ES.Backend.service.JwtService;

@RestController
@RequestMapping("/api/certificate-requests")
public class CertificateRequestController {
    private final CertificateRequestService service;
    private final JwtService jwtService;

    public CertificateRequestController(CertificateRequestService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping("/request")
    public ResponseEntity<CertificateRequest> createRequest(
            @RequestBody CertRequestDTO request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUser(token);
        
        CertificateRequest certificateRequest = service.createRequest(
            request.getCorreo(),
            request.getNombre(),
            request.getOrganizacion(),
            request.getPassword()
        );
        
        return ResponseEntity.ok(certificateRequest);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CertificateRequest>> getPendingRequests(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUser(token);
        
        // Aquí podrías verificar si el usuario es admin
        List<CertificateRequest> pendingRequests = service.getPendingRequests();
        return ResponseEntity.ok(pendingRequests);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CertificateRequest>> getAllRequests(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUser(token);
        
        // Aquí podrías verificar si el usuario es admin
        List<CertificateRequest> allRequests = service.getAllRequests();
        return ResponseEntity.ok(allRequests);
    }

    @GetMapping("/user")
    public ResponseEntity<List<CertificateRequest>> getUserRequests(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUser(token);
        
        List<CertificateRequest> userRequests = service.getRequestsByUser(userEmail);
        return ResponseEntity.ok(userRequests);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<CertificateRequest> approveRequest(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = authHeader.substring(7);
        String adminEmail = jwtService.extractUser(token);
        
        try {
            CertificateRequest approvedRequest = service.approveRequest(id, adminEmail);
            return ResponseEntity.ok(approvedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<CertificateRequest> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectRequestDTO rejectRequest,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = authHeader.substring(7);
        String adminEmail = jwtService.extractUser(token);
        
        CertificateRequest rejectedRequest = service.rejectRequest(id, adminEmail, rejectRequest.reason());
        return ResponseEntity.ok(rejectedRequest);
    }

    public record RejectRequestDTO(String reason) {}
} 