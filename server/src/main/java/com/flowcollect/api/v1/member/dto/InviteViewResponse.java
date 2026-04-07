package com.flowcollect.api.v1.member.dto;

/**
 * Payload returned to the accept-invite page so the UI can display
 * the org name, inviter name, and role before the user fills in their details.
 */
public class InviteViewResponse {

    private String  organizationName;
    private String  inviterName;
    private String  email;
    private String  role;
    private boolean expired;

    public InviteViewResponse() {}

    public InviteViewResponse(String organizationName, String inviterName,
                               String email, String role, boolean expired) {
        this.organizationName = organizationName;
        this.inviterName      = inviterName;
        this.email            = email;
        this.role             = role;
        this.expired          = expired;
    }

    public String  getOrganizationName() { return organizationName; }
    public String  getInviterName()      { return inviterName; }
    public String  getEmail()            { return email; }
    public String  getRole()             { return role; }
    public boolean isExpired()           { return expired; }
}
