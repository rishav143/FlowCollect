package com.paidpeace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Date;
import java.util.UUID;

import com.paidpeace.domain.user.UserRole;
import com.paidpeace.exception.http.UnauthorizedException;

@Service
public class JwtService {

    private static final String ORGANIZATION_ID_CLAIM = "organizationId";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(UUID userId, UUID organizationId, UserRole role) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(role, "role must not be null");

        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ORGANIZATION_ID_CLAIM, organizationId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public JwtClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String orgIdStr = claims.get(ORGANIZATION_ID_CLAIM, String.class);
            String roleStr = claims.get(ROLE_CLAIM, String.class);
            if (orgIdStr == null || orgIdStr.isBlank() || roleStr == null || roleStr.isBlank()) {
                throw new UnauthorizedException("Token is missing required claims");
            }
            UUID organizationId = UUID.fromString(orgIdStr);
            UserRole role = UserRole.valueOf(roleStr);
            return new JwtClaims(userId, organizationId, role);
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("Token has expired");
        } catch (IllegalArgumentException | JwtException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    public long getExpirationMs() {
        return properties.getExpirationMs();
    }

    public record JwtClaims(UUID userId, UUID organizationId, UserRole role) {}
}
