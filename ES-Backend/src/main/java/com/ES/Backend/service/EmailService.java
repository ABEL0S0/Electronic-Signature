package com.ES.Backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ES.Backend.entity.CertificateRequest;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Tu código de verificación");

            String html = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f8; padding: 40px;">
                  <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <img src="https://cdn-icons-png.flaticon.com/512/942/942748.png" alt="Firma electrónica" width="80" style="margin-bottom: 10px;">
                      <h1 style="color: #2b2d42; margin: 0;">¡Bienvenido a E-Signature!</h1>
                    </div>
              
                    <p style="font-size: 16px; color: #444444; text-align: center;">
                      Gracias por registrarte en nuestra <strong>plataforma de firmas electrónicas seguras</strong>.
                    </p>
              
                    <p style="font-size: 16px; color: #444444; text-align: center; margin-top: 20px;">
                      Tu código de verificación es:
                    </p>
              
                    <div style="text-align: center; margin: 30px 0;">
                      <span style="font-size: 32px; color: #1d4ed8; font-weight: bold; letter-spacing: 2px;">%s</span>
                    </div>
              
                    <p style="font-size: 14px; color: #888888; text-align: center;">
                      Si no solicitaste este código, simplemente ignora este correo.
                    </p>
              
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eeeeee;">
              
                    <p style="font-size: 13px; color: #aaa; text-align: center;">
                      © 2025 E-Signature. Todos los derechos reservados.
                    </p>
                  </div>
                </div>
              """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo", e);
        }
    }

    public void sendCertificateRequestNotification(CertificateRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo("admin@e-signature.com"); // Email del administrador
            helper.setSubject("Nueva solicitud de certificado digital");

            String html = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f8; padding: 40px;">
                  <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <img src="https://cdn-icons-png.flaticon.com/512/942/942748.png" alt="Firma electrónica" width="80" style="margin-bottom: 10px;">
                      <h1 style="color: #2b2d42; margin: 0;">Nueva Solicitud de Certificado</h1>
                    </div>
              
                    <p style="font-size: 16px; color: #444444;">
                      Se ha recibido una nueva solicitud de certificado digital:
                    </p>
              
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                      <p><strong>Usuario:</strong> %s</p>
                      <p><strong>Email:</strong> %s</p>
                      <p><strong>Organización:</strong> %s</p>
                      <p><strong>Fecha de solicitud:</strong> %s</p>
                    </div>
              
                    <p style="font-size: 14px; color: #888888;">
                      Por favor, revisa y procesa esta solicitud desde el panel de administración.
                    </p>
              
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eeeeee;">
              
                    <p style="font-size: 13px; color: #aaa; text-align: center;">
                      © 2025 E-Signature. Todos los derechos reservados.
                    </p>
                  </div>
                </div>
              """.formatted(
                request.getUserName(),
                request.getUserEmail(),
                request.getOrganization(),
                request.getRequestedAt().toString()
            );

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo de notificación", e);
        }
    }

    public void sendCertificateApprovedNotification(CertificateRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getUserEmail());
            helper.setSubject("Tu certificado digital ha sido aprobado");

            String html = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f8; padding: 40px;">
                  <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <img src="https://cdn-icons-png.flaticon.com/512/942/942748.png" alt="Firma electrónica" width="80" style="margin-bottom: 10px;">
                      <h1 style="color: #2b2d42; margin: 0;">¡Certificado Aprobado!</h1>
                    </div>
              
                    <p style="font-size: 16px; color: #444444;">
                      Tu solicitud de certificado digital ha sido <strong>aprobada</strong> y ya está disponible en tu cuenta.
                    </p>
              
                    <div style="background-color: #e8f5e8; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745;">
                      <p><strong>Usuario:</strong> %s</p>
                      <p><strong>Organización:</strong> %s</p>
                      <p><strong>Fecha de aprobación:</strong> %s</p>
                    </div>
              
                    <p style="font-size: 14px; color: #888888;">
                      Ya puedes acceder a tu certificado desde la plataforma y comenzar a firmar documentos.
                    </p>
              
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eeeeee;">
              
                    <p style="font-size: 13px; color: #aaa; text-align: center;">
                      © 2025 E-Signature. Todos los derechos reservados.
                    </p>
                  </div>
                </div>
              """.formatted(
                request.getUserName(),
                request.getOrganization(),
                request.getProcessedAt().toString()
            );

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo de notificación", e);
        }
    }

    public void sendCertificateRejectedNotification(CertificateRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getUserEmail());
            helper.setSubject("Tu solicitud de certificado digital ha sido rechazada");

            String html = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f8; padding: 40px;">
                  <div style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <img src="https://cdn-icons-png.flaticon.com/512/942/942748.png" alt="Firma electrónica" width="80" style="margin-bottom: 10px;">
                      <h1 style="color: #2b2d42; margin: 0;">Solicitud Rechazada</h1>
                    </div>
              
                    <p style="font-size: 16px; color: #444444;">
                      Tu solicitud de certificado digital ha sido <strong>rechazada</strong>.
                    </p>
              
                    <div style="background-color: #f8e8e8; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #dc3545;">
                      <p><strong>Usuario:</strong> %s</p>
                      <p><strong>Organización:</strong> %s</p>
                      <p><strong>Fecha de rechazo:</strong> %s</p>
                      <p><strong>Motivo:</strong> %s</p>
                    </div>
              
                    <p style="font-size: 14px; color: #888888;">
                      Si tienes alguna pregunta, por favor contacta con el administrador del sistema.
                    </p>
              
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eeeeee;">
              
                    <p style="font-size: 13px; color: #aaa; text-align: center;">
                      © 2025 E-Signature. Todos los derechos reservados.
                    </p>
                  </div>
                </div>
              """.formatted(
                request.getUserName(),
                request.getOrganization(),
                request.getProcessedAt().toString(),
                request.getRejectionReason()
            );

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("No se pudo enviar el correo de notificación", e);
        }
    }
}
