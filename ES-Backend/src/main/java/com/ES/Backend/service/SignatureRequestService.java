package com.ES.Backend.service;

import java.util.List;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ES.Backend.entity.SignatureRequest;
import com.ES.Backend.entity.SignatureRequestUser;
import com.ES.Backend.entity.User;
import com.ES.Backend.repository.SignatureRequestRepository;
import com.ES.Backend.repository.SignatureRequestUserRepository;
import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.controller.WebSocketController;

@Service
public class SignatureRequestService {
    private final SignatureRequestRepository signatureRequestRepository;
    private final SignatureRequestUserRepository signatureRequestUserRepository;
    private final CertificateService certificateService;
    private final DocumentMetadataService documentMetadataService;
    private final UserService userService;
    
    @Autowired
    private WebSocketController webSocketController;

    public SignatureRequestService(SignatureRequestRepository signatureRequestRepository,
                                   SignatureRequestUserRepository signatureRequestUserRepository,
                                   CertificateService certificateService,
                                   DocumentMetadataService documentMetadataService,
                                   UserService userService) {
        this.signatureRequestRepository = signatureRequestRepository;
        this.signatureRequestUserRepository = signatureRequestUserRepository;
        this.certificateService = certificateService;
        this.documentMetadataService = documentMetadataService;
        this.userService = userService;
    }

    public SignatureRequest createSignatureRequest(SignatureRequest request) {
        if (request.getUsers() != null) {
            for (SignatureRequestUser user : request.getUsers()) {
                user.setSignatureRequest(request);
            }
        }
        
        SignatureRequest savedRequest = signatureRequestRepository.save(request);
        
        // Enviar notificaciones WebSocket a todos los usuarios involucrados
        if (savedRequest.getUsers() != null) {
            for (SignatureRequestUser userRequest : savedRequest.getUsers()) {
                try {
                    User user = userService.findById(userRequest.getUserId());
                    if (user != null) {
                        // Enviar solicitud de firma por WebSocket
                        webSocketController.sendSignatureRequestToUser(user.getEmail(), savedRequest, userRequest);
                        
                        // También enviar notificación general
                        webSocketController.sendNotificationToUser(
                            user.getEmail(), 
                            "Nueva Solicitud de Firma", 
                            "Tienes una nueva solicitud de firma pendiente para el documento: " + 
                            savedRequest.getDocumentPath().substring(savedRequest.getDocumentPath().lastIndexOf("/") + 1)
                        );
                    }
                } catch (Exception e) {
                    System.err.println("Error enviando notificación WebSocket para usuario " + userRequest.getUserId() + ": " + e.getMessage());
                }
            }
        }
        
        return savedRequest;
    }

    public Optional<SignatureRequest> getSignatureRequest(Long id) {
        return signatureRequestRepository.findById(id);
    }

    public List<SignatureRequest> getAllSignatureRequests() {
        return signatureRequestRepository.findAll();
    }

    public SignatureRequestUser saveSignatureRequestUser(SignatureRequestUser user) {
        // Permitir ambos formatos: signatureRequest: { id } o signatureRequestId
        Long srId = findSignatureRequestId(user);
        
        if (srId == null) {
            throw new IllegalArgumentException("signatureRequest o su id es requerido. Recibido: " + user);
        }
        
        SignatureRequest sr = signatureRequestRepository.findById(srId)
            .orElseThrow(() -> new IllegalArgumentException("SignatureRequest no encontrado con ID: " + srId));
        user.setSignatureRequest(sr);
        
        SignatureRequestUser savedUser = signatureRequestUserRepository.save(user);
        
        // Enviar actualización por WebSocket
        try {
            User userEntity = userService.findById(user.getUserId());
            if (userEntity != null) {
                String statusMessage = "PERMITIDO".equals(user.getStatus()) ? 
                    "Has permitido la firma del documento" : 
                    "Has denegado la firma del documento";
                
                webSocketController.sendSignatureRequestUpdate(
                    userEntity.getEmail(), 
                    srId, 
                    user.getStatus(), 
                    statusMessage
                );
            }
        } catch (Exception e) {
            System.err.println("Error enviando actualización WebSocket: " + e.getMessage());
        }
        
        // Obtener la solicitud actualizada
        SignatureRequest request = signatureRequestRepository.findById(savedUser.getSignatureRequest().getId()).orElse(null);
        if (request != null) {
            boolean allResponded = request.getUsers().stream()
                .allMatch(u -> u.getStatus() != null && !u.getStatus().equalsIgnoreCase("PENDIENTE"));
            if (allResponded) {
                try {
                    processSignatures(request.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return savedUser;
    }
    
    /**
     * Busca el ID de la solicitud de firma en el objeto user
     */
    private Long findSignatureRequestId(SignatureRequestUser user) {
        System.out.println("🔍 Buscando signatureRequestId para user: " + user.getId());
        System.out.println("   - signatureRequest: " + user.getSignatureRequest());
        System.out.println("   - signatureRequestId: " + user.getSignatureRequestId());
        
        // Intentar obtener el ID del objeto signatureRequest
        if (user.getSignatureRequest() != null && user.getSignatureRequest().getId() != null) {
            System.out.println("✅ Encontrado ID en signatureRequest: " + user.getSignatureRequest().getId());
            return user.getSignatureRequest().getId();
        }
        
        // Intentar obtener el ID del campo signatureRequestId
        if (user.getSignatureRequestId() != null) {
            System.out.println("✅ Encontrado ID en signatureRequestId: " + user.getSignatureRequestId());
            return user.getSignatureRequestId();
        }
        
        // Si aún no tenemos el ID, intentar obtenerlo del objeto user existente
        if (user.getId() != null) {
            System.out.println("🔍 Buscando en base de datos para user ID: " + user.getId());
            Optional<SignatureRequestUser> existingUser = signatureRequestUserRepository.findById(user.getId());
            if (existingUser.isPresent()) {
                SignatureRequestUser existing = existingUser.get();
                if (existing.getSignatureRequest() != null) {
                    System.out.println("✅ Encontrado ID en base de datos: " + existing.getSignatureRequest().getId());
                    return existing.getSignatureRequest().getId();
                }
            }
        }
        
        System.out.println("❌ No se pudo encontrar signatureRequestId");
        return null;
    }

    public List<SignatureRequestUser> getUsersByRequest(SignatureRequest request) {
        return request.getUsers();
    }


    @Transactional
    public void deleteSignatureRequest(Long id) {
        signatureRequestRepository.deleteById(id);
    }

    /**
     * Procesa la firma grupal: desencripta certificados y firma en orden.
     * Actualiza la metadata del documento como firmado.
     */
    public void processSignatures(Long signatureRequestId) throws Exception {
        SignatureRequest request = signatureRequestRepository.findById(signatureRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de firma no encontrada"));
        List<SignatureRequestUser> users = request.getUsers();
        if (users == null || users.isEmpty()) throw new IllegalStateException("No hay usuarios en la solicitud");

        // Verificar que todos respondieron
        boolean allResponded = users.stream().allMatch(u -> u.getStatus() != null && !u.getStatus().equalsIgnoreCase("PENDIENTE"));
        if (!allResponded) throw new IllegalStateException("No todos los usuarios han respondido");

        // Filtrar los que permitieron y ordenar por el orden que desees (por id, por ejemplo)
        List<SignatureRequestUser> permitted = users.stream()
                .filter(u -> u.getStatus().equalsIgnoreCase("PERMITIDO"))
                .sorted(Comparator.comparing(SignatureRequestUser::getId))
                .toList();
        if (permitted.isEmpty()) throw new IllegalStateException("Ningún usuario permitió la firma");

        // Obtener el documento original
        String pdfInput = request.getDocumentPath();
        String pdfOutput = null;
        String lastSigned = pdfInput;

        for (SignatureRequestUser user : permitted) {
            // 1. Desencriptar certificado
            byte[] p12Bytes = certificateService.decrypt(user.getCertificateId(), user.getCertificatePassword().toCharArray());
            String tempP12Path = "files/uploads/tmp/user_" + user.getUserId() + "_temp.p12";
            Files.write(Paths.get(tempP12Path), p12Bytes);

            // 2. Definir PDF de salida
            pdfOutput = lastSigned.replace(".pdf", "_signed_" + user.getUserId() + ".pdf");

            // 3. Ejecutar script Python
            ProcessBuilder pb = new ProcessBuilder(
                "python", "src/main/java/com/ES/Backend/signer/firmar-pdf.py",
                tempP12Path, user.getCertificatePassword(), lastSigned, pdfOutput,
                String.valueOf(user.getPage()), String.valueOf(user.getPosX()), String.valueOf(user.getPosY()),
                String.valueOf(user.getPosX()+300), String.valueOf(user.getPosY()+100)
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A")) {
                String output = s.hasNext() ? s.next() : "";
                int exitCode = process.waitFor();
                Files.deleteIfExists(Paths.get(tempP12Path));
                if (exitCode != 0) {
                    throw new RuntimeException("Error al firmar PDF para usuario " + user.getUserId() + ": " + output);
                }
            }
            lastSigned = pdfOutput;
        }

        // Al final, reemplazar el original por el firmado final
        Files.deleteIfExists(Paths.get(request.getDocumentPath()));
        Files.move(Paths.get(lastSigned), Paths.get(request.getDocumentPath()));
        // Actualizar metadata del documento como firmado
        DocumentMetadata metadata = null;
        List<DocumentMetadata> docs = documentMetadataService.getDocumentsByUser(users.get(0).getUserId().toString());
        for (DocumentMetadata doc : docs) {
            if (doc.getFilePath().equals(request.getDocumentPath())) {
                metadata = doc;
                break;
            }
        }
        if (metadata != null) {
            metadata.setSigned(true);
            documentMetadataService.updateDocument(metadata);
        }
        // Cambiar estado de la solicitud
        request.setStatus("COMPLETADO");
        signatureRequestRepository.save(request);
        
        // Enviar notificaciones WebSocket de finalización
        for (SignatureRequestUser userRequest : users) {
            try {
                User user = userService.findById(userRequest.getUserId());
                if (user != null) {
                    webSocketController.sendNotificationToUser(
                        user.getEmail(),
                        "Firma Completada",
                        "El documento ha sido firmado exitosamente por todos los usuarios"
                    );
                }
            } catch (Exception e) {
                System.err.println("Error enviando notificación de finalización: " + e.getMessage());
            }
        }
    }
}
