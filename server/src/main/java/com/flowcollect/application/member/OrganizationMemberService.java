package com.flowcollect.application.member;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.member.dto.AcceptInviteRequest;
import com.flowcollect.api.v1.member.dto.InviteMemberRequest;
import com.flowcollect.api.v1.member.dto.InviteViewResponse;
import com.flowcollect.api.v1.member.dto.MemberResponse;
import com.flowcollect.application.auth.LoginResponseFactory;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.user.UserUtil;
import com.flowcollect.domain.invite.InviteStatus;
import com.flowcollect.domain.invite.OrganizationInvite;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.email.ResendEmailClient;
import com.flowcollect.infrastructure.persistence.invite.OrganizationInviteJpaRepository;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;

/**
 * Manages team membership for an organization.
 *
 * <h2>Invite flow</h2>
 * <ol>
 *   <li>Admin calls {@link #inviteMember} → creates a {@link OrganizationInvite} and sends email.</li>
 *   <li>Invitee opens the link, views details via {@link #getInviteView}.</li>
 *   <li>Invitee submits name + password via {@link #acceptInvite} → User created (ACTIVE), JWT returned.</li>
 * </ol>
 *
 * <h2>Constraints</h2>
 * <ul>
 *   <li>Emails are globally unique — a registered user cannot be invited again.</li>
 *   <li>Only one PENDING invite per (org + email) at a time.</li>
 *   <li>The last ADMIN cannot be removed.</li>
 *   <li>A member cannot remove themselves.</li>
 * </ul>
 */
@Service
public class OrganizationMemberService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationMemberService.class);
    private static final int    INVITE_EXPIRY_DAYS = 7;

    private final OrganizationInviteJpaRepository inviteRepository;
    private final UserJpaRepository               userRepository;
    private final OrganizationService             organizationService;
    private final LoginResponseFactory            loginResponseFactory;
    private final ResendEmailClient               emailClient;
    private final NotificationEmailProperties     emailProperties;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public OrganizationMemberService(
            OrganizationInviteJpaRepository inviteRepository,
            UserJpaRepository userRepository,
            OrganizationService organizationService,
            LoginResponseFactory loginResponseFactory,
            ResendEmailClient emailClient,
            NotificationEmailProperties emailProperties
    ) {
        this.inviteRepository     = inviteRepository;
        this.userRepository       = userRepository;
        this.organizationService  = organizationService;
        this.loginResponseFactory = loginResponseFactory;
        this.emailClient          = emailClient;
        this.emailProperties      = emailProperties;
    }

    // -------------------------------------------------------------------------
    // Authenticated endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns all active/inactive members of the org plus any pending invitations.
     * Results are sorted: active members first (by join date), then pending invites.
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID orgId) {
        organizationService.getById(orgId);

        List<MemberResponse> result = new ArrayList<>();

        // Active and inactive users belonging to this org
        userRepository.findAll((root, query, cb) ->
                cb.equal(root.get("organization").get("id"), orgId))
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt))
                .map(MemberResponse::fromUser)
                .forEach(result::add);

        // Pending invites (not yet accepted or revoked)
        inviteRepository.findByOrganizationIdAndStatus(orgId, InviteStatus.PENDING)
                .stream()
                .sorted(Comparator.comparing(OrganizationInvite::getCreatedAt))
                .map(MemberResponse::fromInvite)
                .forEach(result::add);

        return result;
    }

    /**
     * Sends an invitation email to the given address.
     *
     * <p>Guards:
     * <ul>
     *   <li>Email not already a registered user (global uniqueness).</li>
     *   <li>No existing PENDING invite for the same org + email.</li>
     * </ul>
     */
    @Transactional
    public MemberResponse inviteMember(UUID orgId, UUID invitedByUserId, InviteMemberRequest request) {
        Organization org = organizationService.getById(orgId);

        String email = request.getEmail().trim().toLowerCase();
        UserRole role = UserUtil.parseRole(request.getRole());

        // Guard: email already has a registered account (globally unique)
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(
                "A user with email " + email + " is already registered. " +
                "They can be added to this organization directly.");
        }

        // Guard: active invite already pending for this org + email
        if (inviteRepository.existsByOrganizationIdAndEmailAndStatus(orgId, email, InviteStatus.PENDING)) {
            throw new ConflictException(
                "An invitation has already been sent to " + email + ". " +
                "Revoke the existing invite before sending a new one.");
        }

        User invitedBy = userRepository.findByIdAndOrganizationId(invitedByUserId, orgId)
                .orElse(null);

        String token    = UUID.randomUUID().toString().replace("-", "");
        Instant expires = Instant.now().plus(INVITE_EXPIRY_DAYS, ChronoUnit.DAYS);

        OrganizationInvite invite = new OrganizationInvite(org, email, role, token, invitedBy, expires);
        OrganizationInvite saved  = inviteRepository.save(invite);

        sendInviteEmail(email, org.getName(),
                invitedBy != null ? invitedBy.getName() : org.getName(),
                role, token);

        log.info("Invite sent to {} (role={}) for org={} by userId={}",
                email, role, orgId, invitedByUserId);

        return MemberResponse.fromInvite(saved);
    }

    /**
     * Revokes a PENDING invitation. ADMIN only.
     */
    @Transactional
    public void revokeInvite(UUID orgId, UUID inviteId) {
        organizationService.getById(orgId);

        OrganizationInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (!invite.getOrganization().getId().equals(orgId)) {
            throw new NotFoundException("Invitation not found");
        }
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ConflictException("Only PENDING invitations can be revoked");
        }

        invite.revoke();
        inviteRepository.save(invite);
        log.info("Invite revoked: inviteId={}, email={}, org={}", inviteId, invite.getEmail(), orgId);
    }

    /**
     * Removes a member from the organization by deactivating their account.
     *
     * <p>Guards:
     * <ul>
     *   <li>Cannot remove yourself.</li>
     *   <li>Cannot remove the last ADMIN (would lock the org out).</li>
     * </ul>
     */
    @Transactional
    public void removeMember(UUID orgId, UUID requestingUserId, UUID targetUserId) {
        organizationService.getById(orgId);

        if (requestingUserId.equals(targetUserId)) {
            throw new ValidationException("You cannot remove yourself from the organization.");
        }

        User target = userRepository.findByIdAndOrganizationId(targetUserId, orgId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (!target.isActive()) {
            throw new ConflictException("This member is already inactive.");
        }

        // Guard: cannot remove the last active ADMIN
        if (target.getRole() == UserRole.ADMIN) {
            long activeAdminCount = userRepository.findAll((root, query, cb) ->
                    cb.and(
                        cb.equal(root.get("organization").get("id"), orgId),
                        cb.equal(root.get("role"), UserRole.ADMIN),
                        cb.equal(root.get("status"), UserStatus.ACTIVE)
                    )).stream().count();

            if (activeAdminCount <= 1) {
                throw new ConflictException(
                    "Cannot remove the last Admin. " +
                    "Please assign another member as Admin before removing this user.");
            }
        }

        target.deactivate();
        userRepository.save(target);
        log.info("Member deactivated: userId={}, org={}, by={}", targetUserId, orgId, requestingUserId);
    }

    // -------------------------------------------------------------------------
    // Public endpoints (no auth)
    // -------------------------------------------------------------------------

    /**
     * Returns invite details for display on the accept-invite page.
     * Called before the user fills in their name/password.
     */
    @Transactional(readOnly = true)
    public InviteViewResponse getInviteView(String token) {
        OrganizationInvite invite = requireInviteByToken(token);

        String inviterName = invite.getInvitedBy() != null
                ? invite.getInvitedBy().getName()
                : invite.getOrganization().getName();

        return new InviteViewResponse(
                invite.getOrganization().getName(),
                inviterName,
                invite.getEmail(),
                invite.getRole().name(),
                invite.isExpired()
        );
    }

    /**
     * Accepts an invite: creates a User account and returns a JWT for auto-login.
     *
     * @throws ValidationException if the token is invalid, already used, or expired
     * @throws ConflictException   if the email was registered between invite and accept
     */
    @Transactional
    public LoginResponse acceptInvite(String token, AcceptInviteRequest request) {
        OrganizationInvite invite = requireInviteByToken(token);

        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw new ConflictException("This invitation has already been accepted.");
        }
        if (invite.getStatus() == InviteStatus.REVOKED) {
            throw new ValidationException("This invitation has been revoked.");
        }
        if (invite.isExpired()) {
            throw new ValidationException(
                "This invitation has expired. Please ask your administrator to send a new one.");
        }

        // Double-check email uniqueness (race condition guard)
        if (userRepository.existsByEmail(invite.getEmail())) {
            throw new ConflictException(
                "An account with this email address already exists. Please log in instead.");
        }

        String name         = request.getName().trim();
        String passwordHash = UserUtil.hashPassword(request.getPassword());

        User user = new User(
                invite.getOrganization(),
                name,
                invite.getEmail(),
                passwordHash,
                invite.getRole(),
                UserStatus.ACTIVE   // email verified via invite link — no separate verification needed
        );
        User saved = userRepository.save(user);

        invite.accept();
        inviteRepository.save(invite);

        log.info("Invite accepted: email={}, org={}, role={}",
                invite.getEmail(), invite.getOrganization().getId(), invite.getRole());

        return loginResponseFactory.create(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private OrganizationInvite requireInviteByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new NotFoundException("Invitation not found");
        }
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Invitation not found or has expired"));
    }

    private void sendInviteEmail(String to, String orgName, String inviterName,
                                  UserRole role, String token) {
        if (!emailClient.isConfigured()) {
            log.warn("Resend not configured — skipping invite email to {}", to);
            return;
        }

        String fromAddress = emailProperties.getAuthFromAddress();
        String fromName    = emailProperties.getFromName();
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("auth-from-address not configured — skipping invite email");
            return;
        }

        String inviteUrl = frontendUrl + "/accept-invite/" + token;
        String roleLabel = role == UserRole.ADMIN ? "Admin" : "Staff";
        String subject   = inviterName + " invited you to join " + orgName;
        String html      = buildInviteEmailHtml(to, orgName, inviterName, roleLabel, inviteUrl, fromName);

        try {
            emailClient.send(fromName + " <" + fromAddress + ">", to, subject, html);
        } catch (Exception ex) {
            // Don't roll back the invite record if email delivery fails — admin can resend manually
            log.warn("Failed to send invite email to {}: {}", to, ex.getMessage());
            throw new InternalException("Invitation saved but email delivery failed: " + ex.getMessage());
        }
    }

    private String buildInviteEmailHtml(String toEmail, String orgName, String inviterName,
                                         String role, String inviteUrl, String appName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 20px;">
                      <table cellpadding="0" cellspacing="0"
                             style="width:100%%;max-width:560px;background:#ffffff;border-radius:8px;
                                    padding:36px;box-shadow:0 1px 4px rgba(0,0,0,.08);">
                        <tr><td style="font-size:15px;line-height:1.7;color:#374151;">
                          <p style="margin:0 0 16px;">Hi,</p>
                          <p style="margin:0 0 16px;">
                            <strong>%s</strong> has invited you to join <strong>%s</strong>
                            on <strong>%s</strong> as a <strong>%s</strong>.
                          </p>
                          <p style="margin:0 0 24px;">
                            Click the button below to set up your account and get started.
                          </p>
                          <p style="text-align:center;margin:0 0 28px;">
                            <a href="%s"
                               style="background:#29B6F6;color:#ffffff;padding:12px 32px;
                                      border-radius:6px;text-decoration:none;font-weight:bold;
                                      font-size:15px;display:inline-block;">
                              Accept Invitation
                            </a>
                          </p>
                          <p style="color:#6b7280;font-size:13px;margin:0 0 8px;">
                            This invitation expires in 7 days.
                            If you were not expecting this email, you can safely ignore it.
                          </p>
                          <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;">
                          <p style="color:#9ca3af;font-size:12px;margin:0;line-height:1.6;">
                            This is a transactional notification sent by
                            <strong style="color:#9ca3af;">%s</strong>.
                            Please do not reply to this email.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(inviterName, orgName, appName, role, inviteUrl, appName);
    }
}
