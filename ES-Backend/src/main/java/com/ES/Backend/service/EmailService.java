package com.ES.Backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
}
