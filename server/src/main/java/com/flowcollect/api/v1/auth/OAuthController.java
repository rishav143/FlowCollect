package com.flowcollect.api.v1.auth;

import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.auth.dto.OAuthAuthorizeUrlResponse;
import com.flowcollect.application.oauth.OAuthMode;
import com.flowcollect.application.oauth.OAuthService;
import com.flowcollect.domain.oauth.OAuthProvider;
import com.flowcollect.exception.http.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 2.0 login and registration endpoints.
 *
 * <h2>Typical SPA flow</h2>
 * <ol>
 *   <li>Frontend calls {@code GET /authorize-url} to obtain the provider's consent URL.</li>
 *   <li>Frontend redirects the browser to that URL.</li>
 *   <li>Provider redirects back to the frontend {@code redirectUri} with {@code ?code=…&state=…}.</li>
 *   <li>Frontend extracts {@code code} and {@code state}, then calls {@code GET /callback}.</li>
 *   <li>Backend exchanges the code, resolves the user, and returns a JWT.</li>
 * </ol>
 *
 * <p>Both endpoints are explicitly excluded from JWT authentication in {@link com.flowcollect.security.JwtFilter}.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    /**
     * Returns the provider's authorization URL.
     *
     * @param provider    {@code google} or {@code microsoft} (case-insensitive)
     * @param mode        {@code LOGIN} or {@code REGISTER}
     * @param redirectUri the URI the provider should redirect back to (must be registered
     *                    in the provider's OAuth app settings)
     */
    @GetMapping("/{provider}/authorize-url")
    public ResponseEntity<OAuthAuthorizeUrlResponse> getAuthorizeUrl(
            @PathVariable("provider") String provider,
            @RequestParam("mode") String mode,
            @RequestParam("redirectUri") String redirectUri
    ) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        OAuthMode oAuthMode = parseMode(mode);

        String url = oAuthService.buildAuthorizationUrl(oAuthProvider, oAuthMode, redirectUri);
        return ResponseEntity.ok(new OAuthAuthorizeUrlResponse(url));
    }

    /**
     * Handles the OAuth callback: exchanges the authorization code for a JWT.
     *
     * @param provider    {@code google} or {@code microsoft} (case-insensitive)
     * @param code        authorization code returned by the provider
     * @param state       CSRF state token issued by {@code /authorize-url}
     * @param redirectUri must exactly match the URI used in the authorization request
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<LoginResponse> callback(
            @PathVariable("provider") String provider,
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam("redirectUri") String redirectUri
    ) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        LoginResponse response = oAuthService.handleCallback(oAuthProvider, code, state, redirectUri);
        return ResponseEntity.ok(response);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private OAuthProvider parseProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Unsupported OAuth provider '" + provider + "'. Supported: google, microsoft");
        }
    }

    private OAuthMode parseMode(String mode) {
        try {
            return OAuthMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid OAuth mode '" + mode + "'. Supported values: LOGIN, REGISTER");
        }
    }
}
