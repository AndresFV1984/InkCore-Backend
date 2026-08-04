package com.inkcore.application.user.usecase;

import com.inkcore.application.shared.AccessTokenPort;
import com.inkcore.application.shared.PasswordHasherPort;
import com.inkcore.application.shared.RefreshTokenRecord;
import com.inkcore.application.shared.RefreshTokenStorePort;
import com.inkcore.domain.user.exception.InvalidCredentialsException;
import com.inkcore.domain.user.exception.PasswordExpiredException;
import com.inkcore.domain.user.exception.UserLockedException;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import com.inkcore.infrastructure.config.PasswordPolicyProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Login sin {@code @Transactional} envolvente: BCrypt no debe retener conexión JDBC.
 * Lectura y updates usan transacciones cortas en el adapter.
 */
@Service
public class LoginUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenStorePort refreshTokenStore;
    private final PasswordPolicyProperties passwordPolicy;
    private final Clock clock;

    public LoginUserUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            AccessTokenPort accessTokenPort,
            RefreshTokenStorePort refreshTokenStore,
            PasswordPolicyProperties passwordPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenStore = refreshTokenStore;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    public LoginResult execute(LoginUserCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        // JOIN FETCH ya materializa roles/permisos en el agregado (sin lazy fuera de TX).
        User user = userRepository.findByMailIgnoreCase(command.mail())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            long remaining = ChronoUnit.MINUTES.between(now, user.getLockedUntil());
            if (remaining < 1) {
                remaining = 1;
            }
            throw new UserLockedException(remaining);
        }

        if (user.getPasswordExpiresAt() != null && now.isAfter(user.getPasswordExpiresAt())) {
            throw new PasswordExpiredException();
        }

        if (!user.isState()) {
            throw new InvalidCredentialsException();
        }

        // BCrypt fuera de TX JDBC (coste dominante del login).
        if (!passwordHasher.matches(command.rawPassword(), user.getPasswordHash())) {
            User updated = user.registerFailedLogin(
                    passwordPolicy.getMaxFailedAttempts(),
                    passwordPolicy.getLockDurationMinutes(),
                    now
            );
            // Solo columnas de seguridad: no reescribe user_roles.
            userRepository.updateSecurityState(
                    updated.getUserId(),
                    updated.getFailedAttempts(),
                    updated.getLockedUntil(),
                    updated.getLastLoginAt()
            );
            throw new InvalidCredentialsException();
        }

        long accessTtlSeconds = accessTokenPort.getAccessExpirationSeconds();
        long refreshTtlSeconds = accessTokenPort.getRefreshExpirationSeconds();
        String accessToken = accessTokenPort.generateToken(
                user.getUserId(),
                user.getTokenVersion(),
                user.getRoleCodes(),
                user.getPermissionCodes()
        );
        String refreshToken = accessTokenPort.generateRefreshToken();

        User loggedIn = user.registerSuccessfulLogin(now);
        userRepository.updateSecurityState(
                loggedIn.getUserId(),
                loggedIn.getFailedAttempts(),
                loggedIn.getLockedUntil(),
                loggedIn.getLastLoginAt()
        );

        Duration refreshTtl = Duration.ofSeconds(refreshTtlSeconds);
        Instant refreshExpiresAt = Instant.now(clock).plus(refreshTtl);
        refreshTokenStore.save(
                new RefreshTokenRecord(
                        refreshToken,
                        loggedIn.getUserId(),
                        loggedIn.getTokenVersion(),
                        refreshExpiresAt
                ),
                refreshTtl
        );

        return new LoginResult(
                accessToken,
                refreshToken,
                accessTtlSeconds,
                refreshTtlSeconds,
                loggedIn,
                buildPasswordWarning(loggedIn, now),
                buildRoles(loggedIn)
        );
    }

    private LoginResult.PasswordExpirationWarning buildPasswordWarning(User user, LocalDateTime now) {
        if (user.getPasswordExpiresAt() == null) {
            return new LoginResult.PasswordExpirationWarning(false, 0);
        }
        long daysUntilExpiration = ChronoUnit.DAYS.between(
                now.toLocalDate(),
                user.getPasswordExpiresAt().toLocalDate()
        );
        boolean showWarning = daysUntilExpiration >= 0
                && daysUntilExpiration <= passwordPolicy.getWarningDays();
        return new LoginResult.PasswordExpirationWarning(showWarning, Math.max(daysUntilExpiration, 0));
    }

    /**
     * Usa roles/permisos ya cargados en el agregado (JOIN FETCH del login).
     * Evita N+1 a {@code RoleRepositoryPort}.
     */
    private List<LoginResult.RolePermissions> buildRoles(User user) {
        List<String> roleCodes = user.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<String> permissions = user.getPermissionCodes() == null
                ? List.of()
                : List.copyOf(user.getPermissionCodes());
        List<LoginResult.RolePermissions> roles = new ArrayList<>(roleCodes.size());
        for (String roleCode : roleCodes) {
            roles.add(new LoginResult.RolePermissions(roleCode, permissions));
        }
        return List.copyOf(roles);
    }
}
