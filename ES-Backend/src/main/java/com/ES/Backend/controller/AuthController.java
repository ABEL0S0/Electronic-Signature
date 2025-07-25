package com.ES.Backend.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

import com.ES.Backend.entity.User;
import com.ES.Backend.service.JwtService;
import com.ES.Backend.service.UserService;

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
    record AuthResponse(String token, UserResponse user) {}
    record UserResponse(Long id, String firstName, String lastName, String email) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(
                request.firstName(), 
                request.lastName(), 
                request.email(), 
                request.password()
            );
            
            String token = jwtService.generateToken(user.getEmail());
            
            UserResponse userResponse = new UserResponse(
                user.getId(), 
                user.getFirstName(), 
                user.getLastName(), 
                user.getEmail()
            );
            
            return ResponseEntity.ok(new AuthResponse(token, userResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody User request) {
        var user = userService.verifyUserCode(request.getEmail(), request.getVerificationCode());
        if (user != null) {
            return ResponseEntity.ok("Cuenta verificada correctamente");
        } else {
            return ResponseEntity.badRequest().body("Código inválido o usuario no encontrado");
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
                user.getEmail()
            );
            
            return ResponseEntity.ok(new AuthResponse(token, userResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Auth controller is working!";
    }
}