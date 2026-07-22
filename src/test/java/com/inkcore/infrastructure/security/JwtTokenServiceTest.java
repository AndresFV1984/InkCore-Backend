package com.inkcore.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";
    private static final Instant FIXED_NOW = Instant.parse("2026-07-18T12:00:00Z");

    private JwtTokenService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new JwtTokenService(clock, SECRET, 3600, 1209600, objectMapper);
    }

    @Test
    void generateToken_buildsValidHs256JwtWithClaims() throws Exception {
        String jwt = service.generateToken(
                "user-1",
                3L,
                List.of("ADMINISTRADOR"),
                List.of("USUARIO_VER", "USUARIO_CREAR")
        );

        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);

        JsonNode header = objectMapper.readTree(base64UrlDecode(parts[0]));
        assertEquals("HS256", header.get("alg").asText());
        assertEquals("JWT", header.get("typ").asText());

        JsonNode payload = objectMapper.readTree(base64UrlDecode(parts[1]));
        assertEquals("user-1", payload.get("sub").asText());
        assertEquals(FIXED_NOW.getEpochSecond(), payload.get("iat").asLong());
        assertEquals(FIXED_NOW.getEpochSecond() + 3600, payload.get("exp").asLong());
        assertEquals(3L, payload.get("tv").asLong());
        assertEquals("ADMINISTRADOR", payload.get("roles").get(0).asText());
        assertEquals("USUARIO_VER", payload.get("permissions").get(0).asText());
        assertEquals("USUARIO_CREAR", payload.get("permissions").get(1).asText());

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmac(signingInput, SECRET));
        assertEquals(expectedSig, parts[2]);
    }

    @Test
    void generateRefreshToken_isOpaqueBase64UrlWithoutPadding() {
        String token = service.generateRefreshToken();
        assertEquals(43, token.length());
        assertFalse(token.contains("."));
        assertFalse(token.contains("="));
        assertTrue(token.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () ->
                new JwtTokenService(Clock.systemUTC(), "too-short", 3600, 1209600, objectMapper));
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static byte[] hmac(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }
}
