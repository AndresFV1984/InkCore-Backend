package com.inkcore.infrastructure.security;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Precalienta BCrypt y JWT en el arranque para que el primer login no pague el cold-start.
 */
@Component
public class AuthStartupWarmup {

    /**
     * Hash BCrypt strength 10 (mismo coste que el seed admin). Solo para calentar CPU/JIT.
     */
    private static final String WARMUP_BCRYPT_HASH =
            "$2a$10$WGGWmu2DjL2BnBljv70Gzu7TShx6GHdIu/.ivtnSklhahp1zWJwQi";

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthStartupWarmup(PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        passwordEncoder.matches("Indicore2026!", WARMUP_BCRYPT_HASH);
        jwtTokenService.warmUp();
        jwtTokenService.generateToken("warmup", 0L, List.of(), List.of());
        jwtTokenService.generateRefreshToken();
    }
}
