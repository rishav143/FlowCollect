package com.flowcollect.api.v1.member.dto;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.invite.OrganizationInvite;
import com.flowcollect.domain.user.User;

/**
 * Unified response for both active members and pending invitations.
 * The {@code memberType} field distinguishes the two.
 */
public class MemberResponse {

    public enum MemberType { USER, INVITE }

    private UUID    id;
    private String  name;
    private String  email;
    private String  role;
    private String  status;     // ACTIVE | INACTIVE | PENDING
    private MemberType memberType;
    private Instant joinedAt;   // set for USER records
    private Instant invitedAt;  // set for INVITE records

    // Factory methods keep mapping out of the service logic

    public static MemberResponse fromUser(User user) {
        MemberResponse r = new MemberResponse();
        r.id         = user.getId();
        r.name       = user.getName();
        r.email      = user.getEmail();
        r.role       = user.getRole().name();
        r.status     = user.getStatus().name();
        r.memberType = MemberType.USER;
        r.joinedAt   = user.getCreatedAt();
        return r;
    }

    public static MemberResponse fromInvite(OrganizationInvite invite) {
        MemberResponse r = new MemberResponse();
        r.id         = invite.getId();
        r.name       = "";   // name is unknown until the invitee accepts
        r.email      = invite.getEmail();
        r.role       = invite.getRole().name();
        r.status     = "PENDING";
        r.memberType = MemberType.INVITE;
        r.invitedAt  = invite.getCreatedAt();
        return r;
    }

    public UUID       getId()         { return id; }
    public String     getName()       { return name; }
    public String     getEmail()      { return email; }
    public String     getRole()       { return role; }
    public String     getStatus()     { return status; }
    public MemberType getMemberType() { return memberType; }
    public Instant    getJoinedAt()   { return joinedAt; }
    public Instant    getInvitedAt()  { return invitedAt; }
}
