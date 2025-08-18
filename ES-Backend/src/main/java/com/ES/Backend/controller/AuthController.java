package com.ES.Backend.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import com.ES.Backend.entity.User;
import com.ES.Backend.service.JwtService;
import com.ES.Backend.service.UserService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserService userService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    record AuthRequest(String email, String password) {}
    record RegisterRequest(String firstName, String lastName, String email, String password) {}
    record VerifyRequest(String email, String verificationCode) {}
    record AuthResponse(String token, UserResponse user) {}
    record UserResponse(Long id, String firstName, String lastName, String email, String role) {}

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            userService.registerUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body("Verification code sent to email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody VerifyRequest request) {
        var user = userService.verifyUserCode(request.email(), request.verificationCode());
        if (user != null) {
            return ResponseEntity.ok("Cuenta verificada correctamente");
        } else {
            return ResponseEntity.badRequest().body("Código inválido o usuario no encontrado");
        }
    }

    // Password reset request endpoint
    record PasswordResetRequest(String email) {}
    record PasswordResetConfirmRequest(String email, String passwordResetCode, String newPassword) {}
    
    @PostMapping("/password-reset-request")
    public ResponseEntity<String> passwordResetRequest(@RequestBody PasswordResetRequest request) {
        try {
            userService.requestPasswordReset(request.email());
            return ResponseEntity.ok("Password reset code sent to email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Support GET for quick testing via browser using path variable: /api/auth/password-reset-request/{email}
    @GetMapping("/password-reset-request/{email}")
    public ResponseEntity<String> passwordResetRequestGet(@PathVariable("email") String email) {
        try {
            userService.requestPasswordReset(email);
            return ResponseEntity.ok("Password reset code sent to email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Support GET for quick testing via browser using query param: /api/auth/password-reset-request?email=...
    @GetMapping("/password-reset-request")
    public ResponseEntity<String> passwordResetRequestGetQuery(@RequestParam("email") String email) {
        try {
            userService.requestPasswordReset(email);
            return ResponseEntity.ok("Password reset code sent to email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/password-reset")
    public ResponseEntity<String> passwordReset(@RequestBody PasswordResetConfirmRequest request) {
        try {
            userService.resetPassword(request.email(), request.passwordResetCode(), request.newPassword());
            return ResponseEntity.ok("Password updated successfully");
        } catch (com.ES.Backend.service.PasswordResetException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            User user = userService.authenticateUser(request.email(), request.password());
            String token = jwtService.generateToken(user.getEmail());
            UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
            );
            return ResponseEntity.ok(new AuthResponse(token, userResponse));
        } catch (com.ES.Backend.service.EmailNotVerifiedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Auth controller is working!";
    }

    @GetMapping("/websocket-status")
    public String websocketStatus() {
        return "WebSocket endpoint is available at /ws";
    }

    @GetMapping("/health")
    public String health() {
        return "Backend is running!";
    }
}