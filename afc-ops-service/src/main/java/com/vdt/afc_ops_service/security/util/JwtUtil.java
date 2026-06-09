package com.vdt.afc_ops_service.security.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.TokenType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtUtil {

    @NonFinal
    @Value("${app.security.access-token-secret-key}")
    String accessTokenSecretKey;

    public SignedJWT verifyAccessToken(String token) {
        try {
            JWSVerifier verifier = new MACVerifier(accessTokenSecretKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            String tokenType = signedJWT.getJWTClaimsSet().getStringClaim("tokenType");
            boolean verified = signedJWT.verify(verifier);

            if (!verified || expiryTime == null || !expiryTime.after(new Date())
                    || !TokenType.ACCESS_TOKEN.equals(tokenType)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            return signedJWT;
        } catch (JOSEException | ParseException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
