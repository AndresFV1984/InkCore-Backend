package com.indicore.infrastructure.security;

import com.indicore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que el claim {@code tv} coincida con la versión en base de datos (invalidación de sesiones).
 */
public class JwtTokenVersionValidator implements OAuth2TokenValidator<Jwt> {

    private final UserRepositoryPort userRepository;

    public JwtTokenVersionValidator(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sub = token.getSubject();
        if (sub == null) {
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Token sin subject", null));
        }

        Object tvObj = token.getClaim("tv");
        if (!(tvObj instanceof Number claimNum)) {
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Token sin tv", null));
        }
        long claimTv = claimNum.longValue();

        java.util.Optional<Long> currentOpt = userRepository.findTokenVersionByUserId(sub);
        if (currentOpt.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Usuario no encontrado", null));
        }
        if (currentOpt.get().longValue() != claimTv) {
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Token revocado", null));
        }

        return OAuth2TokenValidatorResult.success();
    }
}
