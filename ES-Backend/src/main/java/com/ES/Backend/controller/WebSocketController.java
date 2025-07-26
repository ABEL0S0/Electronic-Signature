package com.ES.Backend.controller;

import com.ES.Backend.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketController extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> userTokens = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionToUser = new ConcurrentHashMap<>(); // sessionId -> userEmail
    private static final Map<String, Set<String>> userToSessions = new ConcurrentHashMap<>(); // userEmail -> Set<sessionId>
    
    @Autowired
    private JwtService jwtService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("✅ Nueva conexión WebSocket establecida: " + session.getId());
        System.out.println("   - URI: " + session.getUri());
        System.out.println("   - Headers: " + session.getHandshakeHeaders());
        sessions.put(session.getId(), session);
        
        // Enviar mensaje de bienvenida
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTION\",\"message\":\"Conexión establecida\",\"sessionId\":\"" + session.getId() + "\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String sessionId = session.getId();
        System.out.println("📨 Mensaje recibido de " + sessionId + ": " + payload);
        
        try {
            // Intentar parsear como JSON
            if (payload.startsWith("{")) {
                JsonNode json = objectMapper.readTree(payload);
                
                // Manejar mensaje de autenticación
                if (json.has("type") && "AUTH".equals(json.get("type").asText())) {
                    String token = json.get("token").asText();
                    String userEmail = jwtService.extractUser(token);
                    
                    // Registrar la sesión del usuario
                    sessionToUser.put(sessionId, userEmail);
                    userTokens.put(sessionId, token);
                    
                    // Agregar sesión al mapeo de usuario
                    userToSessions.computeIfAbsent(userEmail, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
                    
                    System.out.println("🔐 Usuario autenticado: " + userEmail + " en sesión: " + sessionId);
                    System.out.println("📊 Sesiones activas para " + userEmail + ": " + userToSessions.get(userEmail).size());
                    
                    // Confirmar autenticación
                    session.sendMessage(new TextMessage("{\"type\":\"AUTH_SUCCESS\",\"message\":\"Autenticación exitosa\",\"userEmail\":\"" + userEmail + "\"}"));
                    return;
                }
                
                // Otros tipos de mensajes
                session.sendMessage(new TextMessage("Mensaje JSON procesado: " + payload));
            } else {
                // Es un mensaje simple, echo
                session.sendMessage(new TextMessage("Echo: " + payload));
            }
        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje de " + sessionId + ": " + e.getMessage());
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String userEmail = sessionToUser.get(sessionId);
        
        System.out.println("❌ Conexión WebSocket cerrada: " + sessionId);
        System.out.println("   - Usuario: " + (userEmail != null ? userEmail : "Desconocido"));
        System.out.println("   - Código: " + status.getCode());
        System.out.println("   - Razón: " + status.getReason());
        
        // Limpiar sesión
        sessions.remove(sessionId);
        userTokens.remove(sessionId);
        sessionToUser.remove(sessionId);
        
        // Limpiar de múltiples sesiones del usuario
        if (userEmail != null) {
            Set<String> userSessions = userToSessions.get(userEmail);
            if (userSessions != null) {
                userSessions.remove(sessionId);
                if (userSessions.isEmpty()) {
                    userToSessions.remove(userEmail);
                    System.out.println("👤 Usuario " + userEmail + " ya no tiene sesiones activas");
                } else {
                    System.out.println("📊 Usuario " + userEmail + " tiene " + userSessions.size() + " sesiones activas restantes");
                }
            }
        }
    }

    public void sendNotificationToUser(String userEmail, String title, String message) {
        // Encontrar todas las sesiones del usuario específico y enviar notificación
        Set<String> userSessions = userToSessions.get(userEmail);
        if (userSessions != null && !userSessions.isEmpty()) {
            int sentCount = 0;
            for (String sessionId : userSessions) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        String notification = String.format(
                            "{\"type\":\"NOTIFICATION\",\"target\":\"USER\",\"title\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                            title, message, java.time.Instant.now()
                        );
                        session.sendMessage(new TextMessage(notification));
                        sentCount++;
                        System.out.println("✅ Notificación enviada a usuario " + userEmail + " en sesión " + sessionId);
                    } catch (Exception e) {
                        System.err.println("❌ Error enviando notificación a " + userEmail + " en sesión " + sessionId + ": " + e.getMessage());
                    }
                }
            }
            System.out.println("📊 Notificación enviada a " + sentCount + " sesiones de " + userEmail);
        } else {
            System.out.println("⚠️ Usuario " + userEmail + " no está conectado. Notificación no enviada.");
        }
    }

    public void sendNotificationToAdmin(String title, String message) {
        // Enviar notificación a todos los administradores conectados
        int sentCount = 0;
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            try {
                String notification = String.format(
                    "{\"type\":\"NOTIFICATION\",\"target\":\"ADMIN\",\"title\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    title, message, java.time.Instant.now()
                );
                entry.getValue().sendMessage(new TextMessage(notification));
                sentCount++;
            } catch (Exception e) {
                System.err.println("Error enviando notificación: " + e.getMessage());
            }
        }
        System.out.println("📊 Notificación enviada a " + sentCount + " administradores");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("🚨 Error de transporte WebSocket para sesión " + session.getId() + ": " + exception.getMessage());
        exception.printStackTrace();
    }

} 