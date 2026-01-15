package com.sara.ecom.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.admin.expiration:600000}")
    private Long adminExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(String email) {
        // Normalize email to lowercase before generating token
        String normalizedEmail = email != null ? email.toLowerCase().trim() : email;
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, normalizedEmail);
    }
    
    public String generateAdminToken(String email, Long adminId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("type", "admin");
        return createAdminToken(claims, email);
    }
    
    private String createAdminToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + adminExpiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    public boolean isAdminToken(String token) {
        Claims claims = extractAllClaims(token);
        Object type = claims.get("type");
        return "admin".equals(type);
    }
    
    public Long extractAdminId(String token) {
        Claims claims = extractAllClaims(token);
        Object adminId = claims.get("adminId");
        if (adminId instanceof Integer) {
            return ((Integer) adminId).longValue();
        } else if (adminId instanceof Long) {
            return (Long) adminId;
        }
        return null;
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String extractEmail(String token) {
        String email = extractClaim(token, Claims::getSubject);
        // Normalize email to lowercase when extracting from token
        return email != null ? email.toLowerCase().trim() : email;
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        // Normalize both emails for comparison
        String normalizedEmail = email != null ? email.toLowerCase().trim() : email;
        return (tokenEmail.equals(normalizedEmail) && !isTokenExpired(token));
    }
}
