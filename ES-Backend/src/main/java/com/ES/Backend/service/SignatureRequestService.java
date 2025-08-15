package com.ES.Backend.service;

import java.util.List;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ES.Backend.entity.SignatureRequest;
import com.ES.Backend.entity.SignatureRequestUser;
import com.ES.Backend.repository.SignatureRequestRepository;
import com.ES.Backend.repository.SignatureRequestUserRepository;
import com.ES.Backend.entity.DocumentMetadata;

@Service
public class SignatureRequestService {
    private final SignatureRequestRepository signatureRequestRepository;
    private final SignatureRequestUserRepository signatureRequestUserRepository;
    private final CertificateService certificateService;
    private final DocumentMetadataService documentMetadataService;

    public SignatureRequestService(SignatureRequestRepository signatureRequestRepository,
                                   SignatureRequestUserRepository signatureRequestUserRepository,
                                   CertificateService certificateService,
                                   DocumentMetadataService documentMetadataService) {
        this.signatureRequestRepository = signatureRequestRepository;
        this.signatureRequestUserRepository = signatureRequestUserRepository;
        this.certificateService = certificateService;
        this.documentMetadataService = documentMetadataService;
    }

    public SignatureRequest createSignatureRequest(SignatureRequest request) {
        if (request.getUsers() != null) {
            for (SignatureRequestUser user : request.getUsers()) {
                user.setSignatureRequest(request);
            }
        }
        return signatureRequestRepository.save(request);
    }

    public Optional<SignatureRequest> getSignatureRequest(Long id) {
        return signatureRequestRepository.findById(id);
    }

    public List<SignatureRequest> getAllSignatureRequests() {
        return signatureRequestRepository.findAll();
    }

    public SignatureRequestUser saveSignatureRequestUser(SignatureRequestUser user) {
        // Permitir ambos formatos: signatureRequest: { id } o signatureRequestId
        Long srId = null;
        if (user.getSignatureRequest() != null && user.getSignatureRequest().getId() != null) {
            srId = user.getSignatureRequest().getId();
        } else if (user.getSignatureRequestId() != null) {
            srId = user.getSignatureRequestId();
        }
        if (srId == null) {
            throw new IllegalArgumentException("signatureRequest o su id es requerido");
        }
        SignatureRequest sr = signatureRequestRepository.findById(srId)
            .orElseThrow(() -> new IllegalArgumentException("SignatureRequest no encontrado"));
        user.setSignatureRequest(sr);
        return signatureRequestUserRepository.save(user);
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
    }
}
