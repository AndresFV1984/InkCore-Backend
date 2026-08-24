package com.inkcore.application.shared;

import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resuelve la empresa y el usuario del token para no aceptarlos nunca desde el
 * cuerpo de la petición.
 */
@Component
public class AuthenticatedCompanyResolver {

    private final UserRepositoryPort userRepository;

    public AuthenticatedCompanyResolver(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public String resolveCompanyId(Authentication authentication) {
        String userId = resolveUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Sin permiso para el recurso"));
        String companyId = user.getCompanyId();
        if (companyId == null || companyId.isBlank()) {
            throw new AccessDeniedException("Sin permiso para el recurso");
        }
        return companyId;
    }

    public String resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Sin permiso para el recurso");
        }
        Object principal = authentication.getPrincipal();
        String userId = principal instanceof Jwt jwt ? jwt.getSubject() : authentication.getName();
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Sin permiso para el recurso");
        }
        return userId;
    }

    /**
     * Corta el acceso a órdenes de otra empresa.
     */
    public void requireSameCompany(String resourceCompanyId, String authenticatedCompanyId) {
        if (resourceCompanyId == null || !resourceCompanyId.equals(authenticatedCompanyId)) {
            throw new AccessDeniedException("Sin permiso para el recurso");
        }
    }
}
