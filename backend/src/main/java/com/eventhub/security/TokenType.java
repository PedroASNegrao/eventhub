package com.eventhub.security;

/**
 * Distinguishes access tokens (sent with each request) from refresh tokens
 * (only used to mint a new access token), since both are signed with the same key.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
