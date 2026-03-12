package com.flowcollect.security;

import com.flowcollect.api.error.ErrorResponse;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;

@Component
@Order(1)
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern ORGANIZATION_PATH = Pattern.compile("^/api/v1/organizations/([^/]+)(?:/.*)?$");
    private static final List<String> SKIP_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/actuator"
    );

    private final JwtService jwtService;
    private final UserJpaRepository userRepository;
    private final OrganizationJpaRepository organizationRepository;

    public JwtFilter(
            JwtService jwtService,
            UserJpaRepository userRepository,
            OrganizationJpaRepository organizationRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            return true;
        }
        return SKIP_PATHS.stream().anyMatch(path::equalsIgnoreCase);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(AUTHORIZATION_HEADER);
            if (header == null || !header.startsWith(BEARER_PREFIX)) {
                writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                        "Missing or invalid Authorization header");
                return;
            }
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Missing token");
                return;
            }

            JwtService.JwtClaims claims;
            try {
                claims = jwtService.parseToken(token);
                User currentUser = authenticateUser(claims);
                verifyOrganizationAccess(request, claims.organizationId());
                AuthContext.set(new AuthContext(currentUser.getId(), currentUser.getOrganization().getId(), currentUser.getRole()));
                request.setAttribute("auth.userId", currentUser.getId());
                request.setAttribute("auth.organizationId", currentUser.getOrganization().getId());
                request.setAttribute("auth.role", currentUser.getRole());
                filterChain.doFilter(request, response);
            } catch (UnauthorizedException e) {
                writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
            } catch (ForbiddenException e) {
                writeError(response, request, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", e.getMessage());
                return;
            }
        } finally {
            AuthContext.clear();
        }
    }

    private User authenticateUser(JwtService.JwtClaims claims) {
        User user = userRepository.findByIdAndOrganizationId(claims.userId(), claims.organizationId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive");
        }
        Organization organization = organizationRepository.findById(claims.organizationId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated organization no longer exists"));
        
        if (organization.isDeleted()) {
            throw new UnauthorizedException("Authenticated organization is archived");
        }

        return user;
    }

    private void verifyOrganizationAccess(HttpServletRequest request, UUID authenticatedOrganizationId) {
        UUID pathOrganizationId = extractOrganizationIdFromPath(request.getRequestURI());
        if (pathOrganizationId != null && !pathOrganizationId.equals(authenticatedOrganizationId)) {
            throw new ForbiddenException("You cannot access resources from another organization");
        }
    }

    private UUID extractOrganizationIdFromPath(String path) {
        Matcher matcher = ORGANIZATION_PATH.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            int status,
            String code,
            String message
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                code,
                message,
                status,
                Instant.now(),
                request.getRequestURI()
        );
        response.getWriter().write(toJson(body));
    }

    private String toJson(ErrorResponse body) {
        return "{"
                + "\"code\":\"" + escapeJson(body.code()) + "\","
                + "\"message\":\"" + escapeJson(body.message()) + "\","
                + "\"status\":" + body.status() + ","
                + "\"timestamp\":\"" + body.timestamp() + "\","
                + "\"path\":\"" + escapeJson(body.path()) + "\","
                + "\"errors\":[]"
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
