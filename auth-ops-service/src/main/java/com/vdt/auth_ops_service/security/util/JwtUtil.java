package com.vdt.auth_ops_service.security.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vdt.auth_ops_service.constant.TokenType;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import io.jsonwebtoken.*;
import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtUtil {
    public static final String ROLE_PREFIX = "ROLE_";

    @NonFinal
    @Value("${app.security.jwt.access-token-secret-key}")
    String accessTokenSecretKey;

    @NonFinal
    @Value("${app.security.jwt.refresh-token-secret-key}")
    String refreshTokenSecretKey;

    @NonFinal
    @Value("${app.security.jwt.access-token-expiration}")
    long accessTokenExpiration;

    @NonFinal
    @Value("${app.security.jwt.refresh-token-expiration}")
    long refreshTokenExpiration;

    private SecretKey getSecretKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    public String generateToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getId())
                .issuer("vdt.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                    Instant.now().plus(accessTokenExpiration, ChronoUnit.MILLIS).toEpochMilli()
                ))
                .jwtID(java.util.UUID.randomUUID().toString())
                .claim("username", account.getUsername())
                .claim("scope", buildScope(account))
                .claim("tokenType", TokenType.ACCESS_TOKEN)
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(accessTokenSecretKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    public String generateRefreshToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(account.getId())
                .issuer("vdt.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                    Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS).toEpochMilli()
                ))
                .jwtID(java.util.UUID.randomUUID().toString())
                .claim("tokenType", TokenType.REFRESH_TOKEN)
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(refreshTokenSecretKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create refresh token", e);
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    private String buildScope(Account account) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(account.getRoles())) {
            account.getRoles().forEach(role -> {
                stringJoiner.add(ROLE_PREFIX + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
                }
            });
        }

        return stringJoiner.toString();
    }
    
    public SignedJWT verifyAccessToken(String token) {
        return verifyToken(token, accessTokenSecretKey);
    }

    public SignedJWT verifyRefreshToken(String token) {
        return verifyToken(token, refreshTokenSecretKey);
    }

    private SignedJWT verifyToken(String token, String secretKey) {
        try {
            JWSVerifier verifier = new MACVerifier(secretKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            boolean verified = signedJWT.verify(verifier);

            if (!(verified && expiryTime.after(new Date()))) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            return signedJWT;
        } catch (JOSEException | ParseException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
    public Instant getExpirationAtFromAccessToken(String token) {
        return getExpiration(token, accessTokenSecretKey);
    }

    public Instant getExpirationAtFromRefreshToken(String token) {
        return getExpiration(token, refreshTokenSecretKey);
    }

    private Instant getExpiration(String token, String secretKey) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey(secretKey))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .toInstant();
        } catch (Exception e) {
            log.error("Failed to get expiration from token", e);
            throw new AppException(ErrorCode.INVALID_TOKEN_FORMAT);
        }
    }

}
