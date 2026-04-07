package com.flowcollect.api.v1.member;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.member.dto.AcceptInviteRequest;
import com.flowcollect.api.v1.member.dto.InviteViewResponse;
import com.flowcollect.application.member.OrganizationMemberService;

import jakarta.validation.Valid;

/**
 * Public (no-auth) endpoints for the invite accept flow.
 *
 * <p>These are listed under {@code /api/v1/public/invites/} so JwtFilter skips them
 * automatically via the existing {@code /api/v1/public/} prefix rule.
 */
@RestController
@RequestMapping("/api/v1/public/invites")
public class PublicInviteController {

    private final OrganizationMemberService memberService;

    public PublicInviteController(OrganizationMemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Returns invite details (org name, inviter, role, expiry) for the accept-invite page
     * so the UI can show context before the user fills in their name and password.
     */
    @GetMapping("/{token}")
    public ResponseEntity<InviteViewResponse> getInviteView(@PathVariable String token) {
        return ResponseEntity.ok(memberService.getInviteView(token));
    }

    /**
     * Accepts the invitation: creates a User account and returns a JWT for immediate login.
     */
    @PostMapping("/{token}/accept")
    public ResponseEntity<LoginResponse> acceptInvite(
            @PathVariable String token,
            @Valid @RequestBody AcceptInviteRequest request
    ) {
        LoginResponse response = memberService.acceptInvite(token, request);
        return ResponseEntity.ok(response);
    }
}
