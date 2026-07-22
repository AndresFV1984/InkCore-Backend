package com.inkcore.application.user.usecase;

import com.inkcore.application.shared.AccessTokenPort;
import com.inkcore.application.shared.PasswordHasherPort;
import com.inkcore.application.shared.RefreshTokenRecord;
import com.inkcore.application.shared.RefreshTokenStorePort;
import com.inkcore.domain.user.exception.InvalidCredentialsException;
import com.inkcore.domain.user.exception.PasswordExpiredException;
import com.inkcore.domain.user.exception.UserLockedException;
import com.inkcore.domain.user.model.Role;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.RoleRepositoryPort;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import com.inkcore.infrastructure.config.PasswordPolicyProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LoginUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenStorePort refreshTokenStore;
    private final PasswordPolicyProperties passwordPolicy;
    private final Clock clock;

    public LoginUserUseCase(
            UserRepositoryPort userRepository,
            RoleRepositoryPort roleRepository,
            PasswordHasherPort passwordHasher,
            AccessTokenPort accessTokenPort,
            RefreshTokenStorePort refreshTokenStore,
            PasswordPolicyProperties passwordPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenStore = refreshTokenStore;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    @Transactional
    public LoginResult execute(LoginUserCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

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

        if (!passwordHasher.matches(command.rawPassword(), user.getPasswordHash())) {
            User updated = user.registerFailedLogin(
                    passwordPolicy.getMaxFailedAttempts(),
                    passwordPolicy.getLockDurationMinutes(),
                    now
            );
            userRepository.save(updated);
            throw new InvalidCredentialsException();
        }

        List<String> rolesForToken = user.getRoleCodes();
        List<String> permissionsForToken = user.getPermissionCodes();

        String accessToken = accessTokenPort.generateToken(
                user.getUserId(),
                user.getTokenVersion(),
                rolesForToken,
                permissionsForToken
        );
        String refreshToken = accessTokenPort.generateRefreshToken();

        User loggedIn = user.registerSuccessfulLogin(now);
        User saved = userRepository.save(loggedIn);

        Duration refreshTtl = Duration.ofSeconds(accessTokenPort.getRefreshExpirationSeconds());
        Instant refreshExpiresAt = Instant.now(clock).plus(refreshTtl);
        refreshTokenStore.save(
                new RefreshTokenRecord(
                        refreshToken,
                        saved.getUserId(),
                        saved.getTokenVersion(),
                        refreshExpiresAt
                ),
                refreshTtl
        );

        return new LoginResult(
                accessToken,
                refreshToken,
                accessTokenPort.getAccessExpirationSeconds(),
                accessTokenPort.getRefreshExpirationSeconds(),
                saved,
                buildPasswordWarning(saved, now),
                buildRoles(saved)
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

    private List<LoginResult.RolePermissions> buildRoles(User user) {
        List<UUID> roleIds = user.getRoleIds();
        List<String> roleCodes = user.getRoleCodes();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<LoginResult.RolePermissions> roles = new ArrayList<>();
        for (int i = 0; i < roleIds.size(); i++) {
            String roleCode = i < roleCodes.size() ? roleCodes.get(i) : "";
            List<String> permissions = roleRepository.findById(roleIds.get(i))
                    .map(Role::getPermissionCodes)
                    .orElse(List.of());
            roles.add(new LoginResult.RolePermissions(roleCode, permissions));
        }
        return List.copyOf(roles);
    }
}
