package com.ES.Backend.controller;

import com.ES.Backend.entity.SignatureRequest;
import com.ES.Backend.entity.SignatureRequestUser;
import com.ES.Backend.service.SignatureRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/signature-requests")
public class SignatureRequestController {
    private final SignatureRequestService signatureRequestService;

    public SignatureRequestController(SignatureRequestService signatureRequestService) {
        this.signatureRequestService = signatureRequestService;
    }

    // 1. Crear una solicitud de firma grupal
    @PostMapping
    public ResponseEntity<SignatureRequest> createSignatureRequest(@RequestBody SignatureRequest request) {
        SignatureRequest created = signatureRequestService.createSignatureRequest(request);
        return ResponseEntity.ok(created);
    }

    // 2. Obtener una solicitud de firma por ID
    @GetMapping("/{id}")
    public ResponseEntity<SignatureRequest> getSignatureRequest(@PathVariable Long id) {
        Optional<SignatureRequest> request = signatureRequestService.getSignatureRequest(id);
        return request.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3. Listar todas las solicitudes de firma
    @GetMapping
    public List<SignatureRequest> getAllSignatureRequests() {
        return signatureRequestService.getAllSignatureRequests();
    }

    // 4. Guardar o actualizar la respuesta de un usuario a una solicitud
    @PostMapping("/user-response")
    public ResponseEntity<SignatureRequestUser> saveSignatureRequestUser(@RequestBody SignatureRequestUser user) {
        SignatureRequestUser saved = signatureRequestService.saveSignatureRequestUser(user);
        // Obtener la solicitud actualizada
        SignatureRequest request = signatureRequestService.getSignatureRequest(saved.getSignatureRequest().getId()).orElse(null);
        if (request != null) {
            boolean allResponded = request.getUsers().stream()
                .allMatch(u -> u.getStatus() != null && !u.getStatus().equalsIgnoreCase("PENDIENTE"));
            if (allResponded) {
                try {
                    signatureRequestService.processSignatures(request.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ResponseEntity.ok(saved);
    }

    // 5. Obtener los usuarios involucrados en una solicitud
    @GetMapping("/{id}/users")
    public ResponseEntity<List<SignatureRequestUser>> getUsersByRequest(@PathVariable Long id) {
        Optional<SignatureRequest> request = signatureRequestService.getSignatureRequest(id);
        return request.map(r -> ResponseEntity.ok(signatureRequestService.getUsersByRequest(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 6. Eliminar una solicitud de firma
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSignatureRequest(@PathVariable Long id) {
        signatureRequestService.deleteSignatureRequest(id);
        return ResponseEntity.noContent().build();
    }
}
