package com.flowcollect.domain.invite;

public enum InviteStatus {
    /** Invitation sent, awaiting the invitee to accept. */
    PENDING,
    /** Invitee accepted and their account was created. */
    ACCEPTED,
    /** Admin revoked the invitation before it was accepted. */
    REVOKED
}
