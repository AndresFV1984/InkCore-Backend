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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-18T12:00:00Z");
    private static final UUID ROLE_ID = UUID.fromString("b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22");

    @Mock UserRepositoryPort userRepository;
    @Mock RoleRepositoryPort roleRepository;
    @Mock PasswordHasherPort passwordHasher;
    @Mock AccessTokenPort accessTokenPort;
    @Mock RefreshTokenStorePort refreshTokenStore;

    private PasswordPolicyProperties passwordPolicy;
    private LoginUserUseCase useCase;

    @BeforeEach
    void setUp() {
        passwordPolicy = new PasswordPolicyProperties();
        passwordPolicy.setMaxFailedAttempts(5);
        passwordPolicy.setLockDurationMinutes(15);
        passwordPolicy.setWarningDays(7);
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        useCase = new LoginUserUseCase(
                userRepository,
                roleRepository,
                passwordHasher,
                accessTokenPort,
                refreshTokenStore,
                passwordPolicy,
                clock
        );
    }

    @Test
    void login_success_emitsTokensAndResetsFailedAttempts() {
        User user = activeUser(0, null, null);
        when(userRepository.findByMailIgnoreCase("admin@indicolors.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("Indicore2026!", "hash")).thenReturn(true);
        when(accessTokenPort.generateToken(eq("user-1"), eq(1L), any(), any())).thenReturn("access.jwt");
        when(accessTokenPort.generateRefreshToken()).thenReturn("opaque-refresh");
        when(accessTokenPort.getAccessExpirationSeconds()).thenReturn(3600L);
        when(accessTokenPort.getRefreshExpirationSeconds()).thenReturn(1209600L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(
                Role.reconstitute(ROLE_ID, "c1", "Administrador", "", true, List.of("USUARIO_VER"))
        ));

        LoginResult result = useCase.execute(new LoginUserCommand("admin@indicolors.com", "Indicore2026!"));

        assertEquals("access.jwt", result.accessToken());
        assertEquals("opaque-refresh", result.refreshToken());
        assertEquals(3600L, result.accessExpiresInSeconds());
        assertEquals(1209600L, result.refreshExpiresInSeconds());
        assertEquals(0, result.user().getFailedAttempts());
        assertNull(result.user().getLockedUntil());
        assertNotNull(result.user().getLastLoginAt());
        assertEquals(1, result.roles().size());
        assertEquals("ADMINISTRADOR", result.roles().get(0).role());

        ArgumentCaptor<RefreshTokenRecord> refreshCaptor = ArgumentCaptor.forClass(RefreshTokenRecord.class);
        verify(refreshTokenStore).save(refreshCaptor.capture(), eq(Duration.ofSeconds(1209600)));
        assertEquals("opaque-refresh", refreshCaptor.getValue().refreshToken());
        assertEquals("user-1", refreshCaptor.getValue().userId());
        assertEquals(1L, refreshCaptor.getValue().tokenVersion());
    }

    @Test
    void login_wrongPassword_incrementsFailedAttemptsAndThrowsUnauthorized() {
        User user = activeUser(2, null, null);
        when(userRepository.findByMailIgnoreCase("admin@indicolors.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("bad-pass", "hash")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute(new LoginUserCommand("admin@indicolors.com", "bad-pass"))
        );
        assertEquals("UNAUTHORIZED", ex.getCode());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(3, saved.getValue().getFailedAttempts());
        assertNull(saved.getValue().getLockedUntil());
        verify(accessTokenPort, never()).generateToken(any(), any(Long.class), any(), any());
        verify(refreshTokenStore, never()).save(any(), any());
    }

    @Test
    void login_wrongPassword_locksAfterMaxAttempts() {
        User user = activeUser(4, null, null);
        when(userRepository.findByMailIgnoreCase("admin@indicolors.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("bad-pass", "hash")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute(new LoginUserCommand("admin@indicolors.com", "bad-pass"))
        );

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(5, saved.getValue().getFailedAttempts());
        assertEquals(LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC).plusMinutes(15),
                saved.getValue().getLockedUntil());
    }

    @Test
    void login_lockedUser_throwsAccountLocked() {
        LocalDateTime lockedUntil = LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC).plusMinutes(10);
        User user = activeUser(5, lockedUntil, null);
        when(userRepository.findByMailIgnoreCase("admin@indicolors.com")).thenReturn(Optional.of(user));

        UserLockedException ex = assertThrows(
                UserLockedException.class,
                () -> useCase.execute(new LoginUserCommand("admin@indicolors.com", "Indicore2026!"))
        );
        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        assertTrue(ex.getRemainingMinutes() >= 1);
        verify(passwordHasher, never()).matches(any(), any());
    }

    @Test
    void login_expiredPassword_throwsPasswordExpired() {
        LocalDateTime expired = LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC).minusDays(1);
        User user = activeUser(0, null, expired);
        when(userRepository.findByMailIgnoreCase("admin@indicolors.com")).thenReturn(Optional.of(user));

        PasswordExpiredException ex = assertThrows(
                PasswordExpiredException.class,
                () -> useCase.execute(new LoginUserCommand("admin@indicolors.com", "Indicore2026!"))
        );
        assertEquals("PASSWORD_EXPIRED", ex.getCode());
    }

    @Test
    void login_inactiveUser_throwsGenericUnauthorized() {
        User user = User.reconstitute(
                "user-1", "c1", "1", "CC", "Admin", "admin@indicolors.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                false, 1L, List.of(ROLE_ID), List.of("Administrador"), List.of("ADMINISTRADOR"),
                List.of("USUARIO_VER"), false, null, null, 0, null, null
        );
        when(userRepository.findByMailIgnoreCase("admin@indicolors.com")).thenReturn(Optional.of(user));

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute(new LoginUserCommand("admin@indicolors.com", "Indicore2026!"))
        );
        assertEquals("UNAUTHORIZED", ex.getCode());
        verify(passwordHasher, never()).matches(any(), any());
    }

    private static User activeUser(int failedAttempts, LocalDateTime lockedUntil, LocalDateTime passwordExpiresAt) {
        return User.reconstitute(
                "user-1", "c1", "1", "CC", "Admin", "admin@indicolors.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                true, 1L, List.of(ROLE_ID), List.of("Administrador"), List.of("ADMINISTRADOR"),
                List.of("USUARIO_VER"), false, null, passwordExpiresAt, failedAttempts, lockedUntil, null
        );
    }
}
