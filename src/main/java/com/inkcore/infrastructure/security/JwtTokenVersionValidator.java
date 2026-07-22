package com.inkcore.infrastructure.security;

import com.inkcore.application.shared.UserTokenVersionService;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que el claim {@code tv} coincida con la versión vigente del usuario.
 */
public class JwtTokenVersionValidator implements OAuth2TokenValidator<Jwt> {

    private final UserTokenVersionService userTokenVersionService;

    public JwtTokenVersionValidator(UserTokenVersionService userTokenVersionService) {
        this.userTokenVersionService = userTokenVersionService;
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

        try {
            long currentTv = userTokenVersionService.getCurrentTokenVersion(sub);
            if (currentTv != claimTv) {
                return OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Token revocado", null));
            }
            return OAuth2TokenValidatorResult.success();
        } catch (ResourceNotFoundException ex) {
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Usuario no encontrado", null));
        }
    }
}
