package com.flowcollect.api.v1.member;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.member.dto.InviteMemberRequest;
import com.flowcollect.api.v1.member.dto.MemberResponse;
import com.flowcollect.application.member.OrganizationMemberService;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.AuthContext;
import com.flowcollect.security.RequireRole;

import jakarta.validation.Valid;

/**
 * Authenticated endpoints for managing team membership within an organization.
 *
 * <p>All routes are scoped to {@code /api/v1/organizations/{organizationId}/members}
 * and require at least ADMIN role for mutating operations.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
public class OrganizationMemberController {

    private final OrganizationMemberService memberService;

    public OrganizationMemberController(OrganizationMemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Lists all active/inactive members plus pending invitations for the organization.
     */
    @GetMapping
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<List<MemberResponse>> listMembers(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(memberService.listMembers(organizationId));
    }

    /**
     * Sends an invitation email to the given address with the specified role.
     */
    @PostMapping("/invite")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<MemberResponse> invite(
            @PathVariable UUID organizationId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        UUID invitedByUserId = AuthContext.get().getUserId();
        MemberResponse response = memberService.inviteMember(organizationId, invitedByUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Revokes a PENDING invitation.
     */
    @DeleteMapping("/invites/{inviteId}")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID organizationId,
            @PathVariable UUID inviteId
    ) {
        memberService.revokeInvite(organizationId, inviteId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivates an active member. Cannot remove yourself or the last Admin.
     */
    @DeleteMapping("/{userId}")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID organizationId,
            @PathVariable UUID userId
    ) {
        UUID requestingUserId = AuthContext.get().getUserId();
        memberService.removeMember(organizationId, requestingUserId, userId);
        return ResponseEntity.noContent().build();
    }
}
