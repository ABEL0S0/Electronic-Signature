package com.ES.Backend.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${JWT_SECRET:}") String env) {
        if (!env.isBlank()) {
            key = Keys.hmacShaKeyFor(env.getBytes());
        } else {
            key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    public String generateToken(String user) {
        return Jwts.builder()
            .setSubject(user)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 24 * 3600_000))
            .signWith(key)
            .compact();
    }

    public String extractUser(String token) {
        return Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
