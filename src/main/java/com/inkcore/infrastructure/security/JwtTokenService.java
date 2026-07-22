package com.inkcore.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkcore.application.shared.AccessTokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emite access tokens (JWT HS256 manual) y refresh tokens opacos (SecureRandom).
 */
@Component
public class JwtTokenService implements AccessTokenPort {

    private static final String JWT_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final Clock clock;
    private final byte[] secretBytes;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(
            Clock clock,
            @Value("${security.jwt.secret}") String rawSecret,
            @Value("${security.jwt.expiration-seconds:3600}") long accessExpirationSeconds,
            @Value("${security.refresh-token.expiration-seconds:1209600}") long refreshExpirationSeconds,
            ObjectMapper objectMapper
    ) {
        byte[] keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("security.jwt.secret debe tener al menos 32 bytes para HS256");
        }
        this.clock = clock;
        this.secretBytes = keyBytes;
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateToken(String subject, long tokenVersion, List<String> roles, List<String> permissions) {
        long iat = Instant.now(clock).getEpochSecond();
        long exp = iat + accessExpirationSeconds;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", subject);
        payload.put("iat", iat);
        payload.put("exp", exp);
        payload.put("tv", tokenVersion);
        payload.put("roles", roles == null ? List.of() : roles);
        payload.put("permissions", permissions == null ? List.of() : permissions);

        String headerB64 = base64Url(JWT_HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = base64Url(toJsonBytes(payload));
        String signingInput = headerB64 + "." + payloadB64;
        String signatureB64 = base64Url(hmacSha256(signingInput));
        return signingInput + "." + signatureB64;
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return base64Url(bytes);
    }

    @Override
    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    @Override
    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }

    @Override
    public Instant refreshExpiresAt() {
        return Instant.now(clock).plusSeconds(refreshExpirationSeconds);
    }

    private byte[] toJsonBytes(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload JWT", e);
        }
    }

    private byte[] hmacSha256(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo firmar el JWT con HMAC-SHA256", e);
        }
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
