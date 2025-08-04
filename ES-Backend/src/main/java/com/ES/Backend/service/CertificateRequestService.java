package com.ES.Backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ES.Backend.entity.CertificateRequest;
import com.ES.Backend.repository.CertificateRequestRepository;

@Service
public class CertificateRequestService {
    private final CertificateRequestRepository repository;
    private final CertificateService certificateService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public CertificateRequestService(CertificateRequestRepository repository, 
                                   CertificateService certificateService,
                                   EmailService emailService,
                                   NotificationService notificationService) {
        this.repository = repository;
        this.certificateService = certificateService;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    public CertificateRequest createRequest(String userEmail, String userName, String organization, String password) {
        CertificateRequest request = new CertificateRequest(userEmail, userName, organization, password);
        CertificateRequest savedRequest = repository.save(request);
        
        // Enviar notificaciones
        emailService.sendCertificateRequestNotification(savedRequest);
        notificationService.notifyAdminNewCertificateRequest(savedRequest);
        
        return savedRequest;
    }

    public List<CertificateRequest> getPendingRequests() {
        return repository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    public List<CertificateRequest> getRequestsByUser(String userEmail) {
        return repository.findByUserEmail(userEmail);
    }

    public Optional<CertificateRequest> getRequestById(Long id) {
        return repository.findById(id);
    }

    public CertificateRequest approveRequest(Long requestId, String adminEmail) throws Exception {
        CertificateRequest request = repository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("La solicitud ya ha sido procesada");
        }

        // Generar el certificado
        certificateService.generateAndSaveCertificate(
            request.getUserName(),
            request.getUserEmail(),
            request.getOrganization(),
            request.getPassword(),
            request.getUserEmail()
        );

        // Actualizar el estado de la solicitud
        request.setStatus("APPROVED");
        request.setProcessedAt(new java.util.Date());
        request.setProcessedBy(adminEmail);
        
        CertificateRequest updatedRequest = repository.save(request);

        // Enviar notificaciones al usuario
        emailService.sendCertificateApprovedNotification(updatedRequest);
        notificationService.notifyCertificateApproved(updatedRequest);

        return updatedRequest;
    }

    public CertificateRequest rejectRequest(Long requestId, String adminEmail, String reason) {
        CertificateRequest request = repository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("La solicitud ya ha sido procesada");
        }

        request.setStatus("REJECTED");
        request.setProcessedAt(new java.util.Date());
        request.setProcessedBy(adminEmail);
        request.setRejectionReason(reason);
        
        CertificateRequest updatedRequest = repository.save(request);

        // Enviar notificaciones al usuario
        emailService.sendCertificateRejectedNotification(updatedRequest);
        notificationService.notifyCertificateRejected(updatedRequest);

        return updatedRequest;
    }
} 