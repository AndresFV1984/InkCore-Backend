package com.inkcore.application.shared;

/**
 * Versión vigente de tokens de un usuario (invalidación global / logout-all).
 */
public interface UserTokenVersionService {

    /**
     * @return tokenVersion actual del usuario
     * @throws com.inkcore.domain.shared.exception.ResourceNotFoundException si el usuario no existe
     */
    long getCurrentTokenVersion(String userId);
}
