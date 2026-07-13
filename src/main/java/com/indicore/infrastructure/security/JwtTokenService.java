package com.indicore.infrastructure.security;

import com.indicore.application.shared.AccessTokenPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenService implements AccessTokenPort {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String rawSecret,
            @Value("${security.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        byte[] keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("security.jwt.secret debe tener al menos 32 bytes para HS256");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String createAccessToken(String subject, long tokenVersion, List<String> roleCodes) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("tv", tokenVersion)
                .claim("roles", roleCodes == null ? List.of() : roleCodes)
                .build();

        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el JWT", e);
        }
    }

    @Override
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
