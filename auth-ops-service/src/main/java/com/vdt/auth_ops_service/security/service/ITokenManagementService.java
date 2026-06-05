package com.vdt.auth_ops_service.security.service;

import java.time.Instant;

public interface ITokenManagementService {
    void invalidateAccessToken(String token, Instant expireAt);
    boolean isAccessTokenInvalidated(String token);
    void invalidateRefreshToken(String token, Instant expireAt);
    boolean isRefreshTokenInvalidated(String token);
    void invalidateCsrfToken(String token, Instant expireAt);
    boolean isCsrfTokenInvalidated(String token);
}
