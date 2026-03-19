package com.flowcollect.application.oauth;

/**
 * Controls what happens after a successful OAuth provider callback.
 *
 * LOGIN    – the user must already exist in the given organization.
 * REGISTER – a new organization and owner user are created automatically.
 */
public enum OAuthMode {
    LOGIN,
    REGISTER
}
