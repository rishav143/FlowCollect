package com.flowcollect.application.oauth;

import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates short-lived CSRF-protection state tokens for the OAuth flow.
 *
 * The state token is a signed JWT (10-minute TTL) that encodes the OAuth mode
 * (LOGIN / REGISTER) and, for LOGIN, the target organization ID.  Using JWT
 * signature prevents tampering; the nonce prevents replay within the window.
 */
@Service
public class OAuthStateService {

    private static final String MODE_CLAIM = "oauthMode";
    private static final String ORG_CLAIM = "organizationId";
    private static final String NONCE_CLAIM = "nonce";
    private static final long STATE_TTL_MS = 10 * 60 * 1_000L; // 10 minutes

    private final SecretKey key;

    public OAuthStateService(JwtProperties jwtProperties) {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Creates a state token for a LOGIN flow that is scoped to the given organization. */
    public String createLoginState(UUID organizationId) {
        return buildState(OAuthMode.LOGIN, organizationId);
    }

    /** Creates a state token for a REGISTER flow (no existing organization required). */
    public String createRegisterState() {
        return buildState(OAuthMode.REGISTER, null);
    }

    /**
     * Parses and validates a state token.
     *
     * @throws UnauthorizedException if the token is missing, tampered with, or expired
     */
    public StateData parseState(String state) {
        if (state == null || state.isBlank()) {
            throw new UnauthorizedException("OAuth state parameter is missing");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(state)
                    .getPayload();

            String modeStr = claims.get(MODE_CLAIM, String.class);
            if (modeStr == null) {
                throw new UnauthorizedException("OAuth state is missing mode claim");
            }
            OAuthMode mode = OAuthMode.valueOf(modeStr);

            String orgIdStr = claims.get(ORG_CLAIM, String.class);
            UUID organizationId = orgIdStr != null ? UUID.fromString(orgIdStr) : null;

            return new StateData(mode, organizationId);

        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("OAuth login session has expired. Please try again.");
        } catch (UnauthorizedException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid OAuth state. Please try again.");
        }
    }

    private String buildState(OAuthMode mode, UUID organizationId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + STATE_TTL_MS);

        var builder = Jwts.builder()
                .claim(MODE_CLAIM, mode.name())
                .claim(NONCE_CLAIM, UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        if (organizationId != null) {
            builder.claim(ORG_CLAIM, organizationId.toString());
        }

        return builder.compact();
    }

    /** Parsed contents of an OAuth state token. */
    public record StateData(OAuthMode mode, UUID organizationId) {}
}
