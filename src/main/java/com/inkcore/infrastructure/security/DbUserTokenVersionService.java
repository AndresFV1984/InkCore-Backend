package com.inkcore.infrastructure.security;

import com.inkcore.application.shared.UserTokenVersionService;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class DbUserTokenVersionService implements UserTokenVersionService {

    private final UserRepositoryPort userRepository;

    public DbUserTokenVersionService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public long getCurrentTokenVersion(String userId) {
        return userRepository.findTokenVersionByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND",
                        "Usuario no encontrado: " + userId
                ));
    }
}
