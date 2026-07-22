package com.inkcore.application.user.usecase;

import com.inkcore.application.shared.AccessTokenPort;
import com.inkcore.application.shared.RefreshTokenRecord;
import com.inkcore.application.shared.RefreshTokenStorePort;
import com.inkcore.application.shared.UserTokenVersionService;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.exception.InvalidRefreshTokenException;
import com.inkcore.domain.user.exception.UserNotFoundException;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class RefreshAccessTokenUseCase {

    private final RefreshTokenStorePort refreshTokenStore;
    private final UserTokenVersionService userTokenVersionService;
    private final UserRepositoryPort userRepository;
    private final AccessTokenPort accessTokenPort;
    private final boolean rotateOnRefresh;

    public RefreshAccessTokenUseCase(
            RefreshTokenStorePort refreshTokenStore,
            UserTokenVersionService userTokenVersionService,
            UserRepositoryPort userRepository,
            AccessTokenPort accessTokenPort,
            @Value("${security.refresh-token.rotate-on-refresh:true}") boolean rotateOnRefresh
    ) {
        this.refreshTokenStore = refreshTokenStore;
        this.userTokenVersionService = userTokenVersionService;
        this.userRepository = userRepository;
        this.accessTokenPort = accessTokenPort;
        this.rotateOnRefresh = rotateOnRefresh;
    }

    @Transactional
    public RefreshResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshTokenRecord stored = refreshTokenStore.find(refreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        long currentTv;
        try {
            currentTv = userTokenVersionService.getCurrentTokenVersion(stored.userId());
        } catch (ResourceNotFoundException ex) {
            refreshTokenStore.delete(refreshToken);
            throw new UserNotFoundException(stored.userId());
        }

        if (stored.tokenVersion() != currentTv) {
            refreshTokenStore.delete(refreshToken);
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(stored.userId())
                .orElseThrow(() -> new UserNotFoundException(stored.userId()));

        // Códigos de rol para JWT (ROLE_ADMINISTRADOR / PreAuthorize); permisos actuales del usuario.
        List<String> rolesForToken = user.getRoleCodes();
        List<String> permissionsForToken = user.getPermissionCodes();

        String newAccess = accessTokenPort.generateToken(
                stored.userId(),
                stored.tokenVersion(),
                rolesForToken,
                permissionsForToken
        );

        String outRefresh = refreshToken;
        if (rotateOnRefresh) {
            outRefresh = accessTokenPort.generateRefreshToken();
            refreshTokenStore.delete(refreshToken);
            Duration refreshTtl = Duration.ofSeconds(accessTokenPort.getRefreshExpirationSeconds());
            refreshTokenStore.save(
                    new RefreshTokenRecord(
                            outRefresh,
                            stored.userId(),
                            stored.tokenVersion(),
                            accessTokenPort.refreshExpiresAt()
                    ),
                    refreshTtl
            );
        }

        return new RefreshResult(
                newAccess,
                outRefresh,
                accessTokenPort.getAccessExpirationSeconds(),
                accessTokenPort.getRefreshExpirationSeconds()
        );
    }
}
