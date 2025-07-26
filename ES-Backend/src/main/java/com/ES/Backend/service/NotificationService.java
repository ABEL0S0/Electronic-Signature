package com.ES.Backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ES.Backend.entity.CertificateRequest;
import com.ES.Backend.controller.WebSocketController;

@Service
public class NotificationService {

    @Autowired
    private WebSocketController webSocketController;

    public void sendCertificateRequestNotification(CertificateRequest request) {
        // Notificar a todos los administradores
        webSocketController.sendNotificationToAdmin(
            "Nueva solicitud de certificado", 
            "El usuario " + request.getUserName() + " (" + request.getUserEmail() + ") ha solicitado un certificado digital para la organización: " + request.getOrganization()
        );
    }

    public void sendCertificateApprovedNotification(CertificateRequest request) {
        // Notificar al usuario específico
        webSocketController.sendNotificationToUser(
            request.getUserEmail(),
            "✅ Certificado aprobado", 
            "¡Felicidades! Tu solicitud de certificado digital ha sido aprobada. Tu certificado ya está disponible en el sistema."
        );
    }

    public void sendCertificateRejectedNotification(CertificateRequest request) {
        // Notificar al usuario específico
        webSocketController.sendNotificationToUser(
            request.getUserEmail(),
            "❌ Certificado rechazado", 
            "Tu solicitud de certificado digital ha sido rechazada. Motivo: " + request.getRejectionReason() + ". Puedes contactar al administrador para más información."
        );
    }

    public void sendDocumentSignedNotification(String userEmail, String documentName) {
        // Notificar al usuario cuando su documento ha sido firmado
        webSocketController.sendNotificationToUser(
            userEmail,
            "📄 Documento firmado", 
            "El documento '" + documentName + "' ha sido firmado exitosamente."
        );
    }

    public void sendDocumentSignatureRequestNotification(String userEmail, String documentName, String requesterName) {
        // Notificar al usuario cuando se le solicita firmar un documento
        webSocketController.sendNotificationToUser(
            userEmail,
            "✍️ Solicitud de firma", 
            "El usuario " + requesterName + " te ha solicitado firmar el documento '" + documentName + "'."
        );
    }
} 