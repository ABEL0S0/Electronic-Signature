package com.ES.Backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.ES.Backend.data.CertRequestDTO;
import com.ES.Backend.entity.Certificate;
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

    @PostMapping("/request")
    public ResponseEntity<?> generarCertificado(
            @RequestBody CertRequestDTO request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) throws Exception {

        String token = authHeader.substring(7);
        String username = jwtService.extractUser(token);

        // 1. Generar par de claves
        KeyPair keyPair = service.generateRSAKeyPair();

        // 2. Generar certificado X.509 firmado con tu CA
        X509Certificate cert = service.generateUserCertificate(
            request.getNombre(),
            request.getCorreo(),
            request.getOrganizacion(),
            keyPair.getPublic()
        );

        // 3. Crear archivo .p12 protegido con contraseña
        byte[] p12Bytes = service.generarP12(cert, keyPair.getPrivate(), request.getPassword());

        if ("guardar".equalsIgnoreCase(request.getOpcion())) {
            service.guardarCertificadoEnDB(username, p12Bytes, request.getPassword());
            return ResponseEntity.ok(Map.of("message", "Certificado guardado correctamente"));
        } else {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=firma_electronica.p12")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(p12Bytes);
        }
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

    @GetMapping("/user/{user}")
    public List<Certificate> getCertificatesByUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String user
    ) {
        String token = authHeader.substring(7);
        jwtService.extractUser(token);
        return service.getCertificatesByUser(user);
    }

    @DeleteMapping("/user/{user}")
    public void deleteCertificatesByUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String user
    ) {
        String token = authHeader.substring(7);
        jwtService.extractUser(token);
        service.deleteCertificatesByUser(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCertificate(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
        @PathVariable String id
    ) {
        String token = authHeader.substring(7);
        String user = jwtService.extractUser(token);

        return service.getCertificateById(id)
            .map(cert -> {
                if (!cert.getuser().equals(user)) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
                service.deleteCertificate(id);
                return new ResponseEntity<>(HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}