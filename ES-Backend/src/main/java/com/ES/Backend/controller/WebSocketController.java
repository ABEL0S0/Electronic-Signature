package com.ES.Backend.controller;

import com.ES.Backend.service.JwtService;
import com.ES.Backend.service.UserService;
import com.ES.Backend.entity.User;
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
    private static final Map<String, String> sessionToUserRole = new ConcurrentHashMap<>(); // sessionId -> userRole
    private static final Map<String, Set<String>> userToSessions = new ConcurrentHashMap<>(); // userEmail -> Set<sessionId>
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserService userService;
    
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
                
                // Manejar mensaje de autenticación (requerido)
                if (json.has("type") && "AUTH".equals(json.get("type").asText())) {
                    String token = json.get("token").asText();
                    String userEmail = jwtService.extractUser(token);
                    
                    // Obtener información completa del usuario incluyendo el rol
                    User user = userService.findByEmail(userEmail);
                    String userRole = user.getRole();
                    
                    // Registrar la sesión del usuario
                    sessionToUser.put(sessionId, userEmail);
                    sessionToUserRole.put(sessionId, userRole);
                    userTokens.put(sessionId, token);
                    
                    // Agregar sesión al mapeo de usuario
                    userToSessions.computeIfAbsent(userEmail, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
                    
                    System.out.println("🔐 Usuario autenticado: " + userEmail + " (Rol: " + userRole + ") en sesión: " + sessionId);
                    System.out.println("📊 Sesiones activas para " + userEmail + ": " + userToSessions.get(userEmail).size());
                    
                    // Confirmar autenticación
                    session.sendMessage(new TextMessage("{\"type\":\"AUTH_SUCCESS\",\"message\":\"Autenticación exitosa\",\"userEmail\":\"" + userEmail + "\",\"userRole\":\"" + userRole + "\"}"));
                    return;
                }
                
                // Verificar si el usuario está autenticado para otros mensajes
                String userEmail = sessionToUser.get(sessionId);
                if (userEmail == null) {
                    session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Usuario no autenticado\"}"));
                    return;
                }
                
                // Manejar otros tipos de mensajes aquí si es necesario
                session.sendMessage(new TextMessage("{\"type\":\"MESSAGE_RECEIVED\",\"message\":\"Mensaje procesado correctamente\"}"));
                
            } else {
                // Mensaje de texto plano
                session.sendMessage(new TextMessage("{\"type\":\"ECHO\",\"message\":\"" + payload + "\"}"));
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje: " + e.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Error procesando mensaje: " + e.getMessage() + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String userEmail = sessionToUser.get(sessionId);
        
        System.out.println("❌ Conexión WebSocket cerrada: " + sessionId + " - Usuario: " + userEmail);
        
        // Limpiar datos de la sesión
        sessions.remove(sessionId);
        userTokens.remove(sessionId);
        sessionToUser.remove(sessionId);
        sessionToUserRole.remove(sessionId);
        
        // Remover sesión del mapeo de usuario
        if (userEmail != null) {
            Set<String> userSessions = userToSessions.get(userEmail);
            if (userSessions != null) {
                userSessions.remove(sessionId);
                if (userSessions.isEmpty()) {
                    userToSessions.remove(userEmail);
                }
            }
        }
    }

    /**
     * Envía notificación a un usuario específico
     */
    public void sendNotificationToUser(String userEmail, String title, String message) {
        Set<String> userSessions = userToSessions.get(userEmail);
        if (userSessions != null && !userSessions.isEmpty()) {
            for (String sessionId : userSessions) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        String notification = String.format(
                            "{\"type\":\"NOTIFICATION\",\"target\":\"USER\",\"title\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                            title, message, java.time.Instant.now()
                        );
                        session.sendMessage(new TextMessage(notification));
                    } catch (Exception e) {
                        System.err.println("❌ Error enviando notificación a " + userEmail + " en sesión " + sessionId + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Envía notificación solo a usuarios con rol ADMIN
     */
    public void sendNotificationToAdmin(String title, String message) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();
            
            // Verificar si el usuario de esta sesión tiene rol ADMIN
            String userRole = sessionToUserRole.get(sessionId);
            if ("ADMIN".equals(userRole) && session.isOpen()) {
                try {
                    String notification = String.format(
                        "{\"type\":\"NOTIFICATION\",\"target\":\"ADMIN\",\"title\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                        title, message, java.time.Instant.now()
                    );
                    session.sendMessage(new TextMessage(notification));
                } catch (Exception e) {
                    System.err.println("❌ Error enviando notificación de admin a sesión " + sessionId + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Obtiene el número de sesiones activas
     */
    public int getActiveSessionsCount() {
        return sessions.size();
    }

    /**
     * Obtiene el número de usuarios únicos conectados
     */
    public int getUniqueUsersCount() {
        return userToSessions.size();
    }
} 