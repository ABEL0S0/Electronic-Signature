package com.ES.Backend.controller;

import com.ES.Backend.entity.Certificate;
import com.ES.Backend.entity.DocumentMetadata;
import com.ES.Backend.service.CertificateService;
import com.ES.Backend.service.DocumentMetadataService;
import com.ES.Backend.service.JwtService;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentMetadataController {

    @Value("${tmp.path}")
    private String tmpPath;

    private final DocumentMetadataService service;
    private final CertificateService certificateService;
    private final JwtService jwtService;

    public DocumentMetadataController(DocumentMetadataService service, JwtService jwtService, CertificateService certificateService) {
        this.service = service;
        this.jwtService = jwtService;
        this.certificateService = certificateService;
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
        try {
            System.out.println("Attempting to delete document with ID: " + id);
            String token = authHeader.substring(7);
            String user = jwtService.extractUser(token);
            
            DocumentMetadata metadata = service.downloadDocument(id);
            if (metadata == null) {
                System.err.println("Document not found: " + id);
                return ResponseEntity.notFound().build();
            }

            if (!metadata.getuser().equals(user)) {
                System.err.println("Unauthorized deletion attempt by user " + user + " for document " + id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to delete this document");
            }
            
            service.deleteDocument(id);
            System.out.println("Document successfully deleted: " + id);
            return ResponseEntity.ok().build();
            
        } catch (ResponseStatusException e) {
            System.err.println("Response status exception: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            System.err.println("IO Exception while deleting document: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error deleting document file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error while deleting document: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Unexpected error: " + e.getMessage());
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

    @GetMapping("/search")
    public ResponseEntity<?> searchDocumentByPath(
        @RequestParam String filepath
    ) {
        try {
            DocumentMetadata metadata = service.findDocumentByPath(filepath);
            if (metadata != null) {
                return ResponseEntity.ok(metadata);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/sign/{id}")
    public ResponseEntity<?> signDocument(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String id,
        @RequestParam int page,
        @RequestParam float x,
        @RequestParam float y,
        @RequestParam String certificateId,
        @RequestParam String certPassword // o como lo manejes
    ) {
        try {
            System.out.println("[signDocument] Iniciando firma de documento. ID: " + id + ", CertID: " + certificateId);
            String token = authHeader.substring(7);
            String user = jwtService.extractUser(token);
            System.out.println("[signDocument] Usuario extraído del token: " + user);

            // 1. Obtener metadata y verificar usuario
            DocumentMetadata metadata = service.downloadDocument(id);
            System.out.println("[signDocument] Metadata obtenida: " + metadata.getFileName());
            if (!metadata.getuser().equals(user)) {
                System.out.println("[signDocument] Usuario no autorizado para este documento.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
            }

            // 2. Obtener el certificado de la base de datos
            Optional<Certificate> certOpt = certificateService.getCertificateById(certificateId);
            if (certOpt.isEmpty()) {
                System.out.println("[signDocument] Certificado no encontrado en la base de datos.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificado no encontrado");
            }
            Certificate certEntity = certOpt.get();
            System.out.println("[signDocument] Certificado encontrado: " + certEntity.getFilename());

            // 3. Descifrar el .p12
            byte[] p12Bytes = certificateService.decrypt(certificateId, certPassword.toCharArray());
            System.out.println("[signDocument] Certificado descifrado, tamaño: " + p12Bytes.length + " bytes");
            String rootPath = Paths.get("files").toAbsolutePath().toString();
            String tempP12Path = rootPath + java.io.File.separator + tmpPath + java.io.File.separator + user + "_temp.p12";
            System.out.println("[signDocument] Ruta temporal para .p12: " + tempP12Path);
            java.nio.file.Files.write(java.nio.file.Paths.get(tempP12Path), p12Bytes);

            String pdfInput = metadata.getFilePath();
            String pdfOutput = pdfInput.replace(".pdf", "_signed.pdf");
            //String stylePath = "src/main/java/com/ES/Backend/signer/stamp-style.yml";
            System.out.println("[signDocument] PDF de entrada: " + pdfInput);
            System.out.println("[signDocument] PDF de salida: " + pdfOutput);
            //System.out.println("[signDocument] Style YAML: " + stylePath);

            // 4. Ejecutar script de Python
            ProcessBuilder pb = new ProcessBuilder(
                "python", "src/main/java/com/ES/Backend/signer/firmar-pdf.py",
                tempP12Path, certPassword, pdfInput, pdfOutput,
                String.valueOf(page), String.valueOf(x), String.valueOf(y),
                String.valueOf(x+300), String.valueOf(y+100)
            );
            pb.redirectErrorStream(true);
            System.out.println("[signDocument] Ejecutando script Python: " + pb.command());
            Process process = pb.start();
            java.io.InputStream is = process.getInputStream();
            String output;
            try (java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
                output = s.hasNext() ? s.next() : "";
            }
            System.out.println("[signDocument] Salida del script Python:\n" + output);
            int exitCode = process.waitFor();
            System.out.println("[signDocument] Código de salida del script: " + exitCode);

            // 5. Borrar el archivo temporal
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempP12Path));
            System.out.println("[signDocument] Archivo temporal eliminado: " + tempP12Path);

            if (exitCode != 0) {
                System.out.println("[signDocument] Error al firmar PDF");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al firmar PDF");
            }

            // 6. Reemplazar el archivo original por el firmado y actualizar metadata
            try {
                // Eliminar el original
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(pdfInput));
                // Renombrar el firmado al nombre original
                java.nio.file.Files.move(
                    java.nio.file.Paths.get(pdfOutput),
                    java.nio.file.Paths.get(pdfInput)
                );
                System.out.println("[signDocument] Archivo firmado reemplazó al original: " + pdfInput);
            } catch (Exception e) {
                System.out.println("[signDocument] Error al reemplazar el archivo original: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Error al reemplazar el archivo original: " + e.getMessage());
            }

            metadata.setFilePath(pdfInput);
            metadata.setSigned(true);
            service.updateDocument(metadata);
            System.out.println("[signDocument] Documento firmado y metadata actualizada.");

            return ResponseEntity.ok("Documento firmado correctamente");
        } catch (Exception e) {
            System.out.println("[signDocument] Excepción: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}