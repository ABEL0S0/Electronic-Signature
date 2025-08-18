package com.ES.Backend.controller;

import com.ES.Backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Trigger status notifications for a user
     */
    @PostMapping("/trigger-status")
    public ResponseEntity<Map<String, String>> triggerStatusNotifications(
            @RequestBody Map<String, String> request) {
        String userEmail = request.get("userEmail");
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userEmail es requerido"));
        }
        
        try {
            notificationService.checkAndNotifyUserStatus(userEmail);
            return ResponseEntity.ok(Map.of("success", "true", "message", "Notificaciones de estado enviadas"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error enviando notificaciones: " + e.getMessage()));
        }
    }


} 