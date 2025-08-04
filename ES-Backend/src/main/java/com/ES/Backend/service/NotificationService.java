package com.ES.Backend.service;

import com.ES.Backend.controller.WebSocketController;
import com.ES.Backend.entity.Certificate;
import com.ES.Backend.entity.CertificateRequest;
import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.entity.User;
import com.ES.Backend.repository.CertificateRepository;
import com.ES.Backend.repository.DocumentMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DocumentMetadataRepository documentMetadataRepository;

    /**
     * Envía notificación cuando se aprueba una solicitud de certificado
     */
    public void notifyCertificateApproved(CertificateRequest request) {
        String userEmail = request.getUserEmail();
        String title = "Certificado Aprobado";
        String message = "Tu solicitud de certificado digital ha sido aprobada. Ya puedes firmar documentos.";
        
        webSocketController.sendNotificationToUser(userEmail, title, message);
    }

    /**
     * Envía notificación cuando se rechaza una solicitud de certificado
     */
    public void notifyCertificateRejected(CertificateRequest request) {
        String userEmail = request.getUserEmail();
        String title = "Certificado Rechazado";
        String message = "Tu solicitud de certificado digital ha sido rechazada. Contacta al administrador para más información.";
        
        webSocketController.sendNotificationToUser(userEmail, title, message);
    }

    /**
     * Envía notificación cuando un usuario tiene documentos pendientes de firma
     */
    public void notifyPendingDocuments(String userEmail) {
        List<DocumentMetadata> allDocs = documentMetadataRepository.findByuser(userEmail);
        List<DocumentMetadata> pendingDocs = allDocs.stream()
            .filter(doc -> !doc.isSigned())
            .toList();
        
        if (!pendingDocs.isEmpty()) {
            String title = "Documentos Pendientes";
            String message = "Tienes " + pendingDocs.size() + " documento(s) pendiente(s) de firma.";
            
            webSocketController.sendNotificationToUser(userEmail, title, message);
        }
    }

    /**
     * Envía notificación cuando un usuario no tiene certificados
     */
    public void notifyNoCertificates(String userEmail) {
        List<Certificate> certificates = certificateRepository.findByuser(userEmail);
        
        if (certificates.isEmpty()) {
            String title = "Certificado Requerido";
            String message = "Necesitas un certificado digital para firmar documentos. Solicita uno desde el dashboard.";
            
            webSocketController.sendNotificationToUser(userEmail, title, message);
        }
    }

    /**
     * Envía notificación cuando un usuario no tiene documentos
     */
    public void notifyNoDocuments(String userEmail) {
        List<DocumentMetadata> documents = documentMetadataRepository.findByuser(userEmail);
        
        if (documents.isEmpty()) {
            String title = "Sin Documentos";
            String message = "No tienes documentos cargados. Sube tu primer documento para comenzar.";
            
            webSocketController.sendNotificationToUser(userEmail, title, message);
        }
    }

    /**
     * Envía notificación a administradores sobre nueva solicitud de certificado
     */
    public void notifyAdminNewCertificateRequest(CertificateRequest request) {
        String title = "Nueva Solicitud de Certificado";
        String message = "El usuario " + request.getUserEmail() + " ha solicitado un certificado digital.";
        
        webSocketController.sendNotificationToAdmin(title, message);
    }

    /**
     * Verifica y envía notificaciones de estado al usuario
     */
    public void checkAndNotifyUserStatus(String userEmail) {
        // Verificar si no tiene certificados
        notifyNoCertificates(userEmail);
        
        // Verificar si no tiene documentos
        notifyNoDocuments(userEmail);
        
        // Verificar documentos pendientes
        notifyPendingDocuments(userEmail);
    }
} 