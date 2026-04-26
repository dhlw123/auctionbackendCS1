package com.auction.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private int jwtExpirationms;
    @Value("${jwt.refreshExpiration}") // 7 days
    private int jwtRefreshExpirationms;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates an access token for the given username.
     *
     * @param username The username to generate the token for.
     * @return The generated access token.
     */
    public String generateToken(String username) {
        return Jwts.builder().subject(username).issuedAt(new Date())
                .expiration(new Date((new Date().getTime()) + jwtExpirationms)).signWith(key).compact();
    }

    /**
     * Generates a refresh token for the given username.
     *
     * @param username The username to generate the token for.
     * @return The generated refresh token.
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder().subject(username).issuedAt(new Date())
                .expiration(new Date((new Date().getTime()) + jwtRefreshExpirationms)).signWith(key).compact();
    }

    public String getUserFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.err.printf("JWT validation error: %s", e.getMessage());

        }
        return false;
    }
}
