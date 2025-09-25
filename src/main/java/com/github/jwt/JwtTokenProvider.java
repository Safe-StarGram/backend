package com.github.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    private Algorithm algorithm() {
        return Algorithm.HMAC256(props.getSecret());
    }

    private JWTVerifier verifier() {
        return JWT.require(algorithm())
                .build();
    }

    public String generateAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTtl());
        return JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(String.valueOf(userId))
                .withClaim("role", role)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .sign(algorithm());
    }

    public String generateRefreshToken(Long userId, String role, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getRefreshTtl());  // 예: 14d
        return JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(String.valueOf(userId))
                .withClaim("role", role)
                .withJWTId(jti)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .sign(algorithm());
    }

    public boolean validate(String token) {
        try { 
            verifier().verify(token); 
            return true; 
        }
        catch (Exception e) { 
            System.out.println("JWT Validation Error: " + e.getMessage());
            return false; 
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            DecodedJWT decodedJWT = verifier().verify(token);
            // refresh token은 JTI가 있어야 함
            String jti = decodedJWT.getId();
            if (jti == null || jti.trim().isEmpty()) {
                System.out.println("Refresh token에 JTI가 없습니다.");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("Refresh Token Validation Error: " + e.getMessage());
            return false;
        }
    }

    public DecodedJWT decode(String token) {
        try {
            return verifier().verify(token);
        } catch (Exception e) {
            System.out.println("Token decode error: " + e.getMessage());
            throw new RuntimeException("Token cannot be decoded: " + e.getMessage());
        }
    }

    public Long getUserId(String token) {
        try {
            String subject = decode(token).getSubject();
            return Long.valueOf(subject);
        } catch (Exception e) {
            System.out.println("Error getting userId from token: " + e.getMessage());
            throw new RuntimeException("Failed to extract userId from token: " + e.getMessage());
        }
    }

    public String getRole(String token) {
        try {
            return decode(token).getClaim("role").asString();
        } catch (Exception e) {
            System.out.println("Error getting role from token: " + e.getMessage());
            throw new RuntimeException("Failed to extract role from token: " + e.getMessage());
        }
    }

    public String getJti(String token) {
        try {
            return decode(token).getId();
        } catch (Exception e) {
            System.out.println("Error getting jti from token: " + e.getMessage());
            throw new RuntimeException("Failed to extract jti from token: " + e.getMessage());
        }
    }

    public String newJti() {
        return UUID.randomUUID().toString();
    }

}
