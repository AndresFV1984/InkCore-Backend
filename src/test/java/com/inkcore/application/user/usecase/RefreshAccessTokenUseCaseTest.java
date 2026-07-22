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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenUseCaseTest {

    private static final String OLD_REFRESH = "old-refresh-token-opaque";
    private static final String USER_ID = "user-1";
    private static final long TOKEN_VERSION = 3L;

    @Mock RefreshTokenStorePort refreshTokenStore;
    @Mock UserTokenVersionService userTokenVersionService;
    @Mock UserRepositoryPort userRepository;
    @Mock AccessTokenPort accessTokenPort;

    @BeforeEach
    void commonStubs() {
        // no-op; stubs per test
    }

    @Test
    void refresh_successWithoutRotation_keepsSameRefresh() {
        RefreshAccessTokenUseCase useCase = newUseCase(false);
        RefreshTokenRecord stored = storedRecord();
        when(refreshTokenStore.find(OLD_REFRESH)).thenReturn(Optional.of(stored));
        when(userTokenVersionService.getCurrentTokenVersion(USER_ID)).thenReturn(TOKEN_VERSION);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser()));
        when(accessTokenPort.generateToken(eq(USER_ID), eq(TOKEN_VERSION), any(), any())).thenReturn("new.access.jwt");
        when(accessTokenPort.getAccessExpirationSeconds()).thenReturn(3600L);
        when(accessTokenPort.getRefreshExpirationSeconds()).thenReturn(1209600L);

        RefreshResult result = useCase.refresh(OLD_REFRESH);

        assertEquals("new.access.jwt", result.accessToken());
        assertEquals(OLD_REFRESH, result.refreshToken());
        assertEquals(3600L, result.tokenAccesExpira());
        assertEquals(1209600L, result.tokenRefresExpira());
        verify(refreshTokenStore, never()).delete(any());
        verify(refreshTokenStore, never()).save(any(), any());
        verify(accessTokenPort, never()).generateRefreshToken();
    }

    @Test
    void refresh_successWithRotation_deletesOldAndSavesNew() {
        RefreshAccessTokenUseCase useCase = newUseCase(true);
        RefreshTokenRecord stored = storedRecord();
        Instant expiresAt = Instant.parse("2026-08-01T00:00:00Z");
        when(refreshTokenStore.find(OLD_REFRESH)).thenReturn(Optional.of(stored));
        when(userTokenVersionService.getCurrentTokenVersion(USER_ID)).thenReturn(TOKEN_VERSION);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser()));
        when(accessTokenPort.generateToken(eq(USER_ID), eq(TOKEN_VERSION), any(), any())).thenReturn("new.access.jwt");
        when(accessTokenPort.generateRefreshToken()).thenReturn("new-opaque-refresh");
        when(accessTokenPort.refreshExpiresAt()).thenReturn(expiresAt);
        when(accessTokenPort.getAccessExpirationSeconds()).thenReturn(3600L);
        when(accessTokenPort.getRefreshExpirationSeconds()).thenReturn(1209600L);

        RefreshResult result = useCase.refresh(OLD_REFRESH);

        assertEquals("new.access.jwt", result.accessToken());
        assertEquals("new-opaque-refresh", result.refreshToken());
        assertNotEquals(OLD_REFRESH, result.refreshToken());

        verify(refreshTokenStore).delete(OLD_REFRESH);
        ArgumentCaptor<RefreshTokenRecord> captor = ArgumentCaptor.forClass(RefreshTokenRecord.class);
        verify(refreshTokenStore).save(captor.capture(), eq(Duration.ofSeconds(1209600)));
        assertEquals("new-opaque-refresh", captor.getValue().refreshToken());
        assertEquals(USER_ID, captor.getValue().userId());
        assertEquals(TOKEN_VERSION, captor.getValue().tokenVersion());
        assertEquals(expiresAt, captor.getValue().expiresAt());
    }

    @Test
    void refresh_notFound_throwsInvalidRefreshToken() {
        RefreshAccessTokenUseCase useCase = newUseCase(true);
        when(refreshTokenStore.find(OLD_REFRESH)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.refresh(OLD_REFRESH));
        verify(accessTokenPort, never()).generateToken(any(), anyLong(), any(), any());
    }

    @Test
    void refresh_tokenVersionMismatch_deletesAndThrows() {
        RefreshAccessTokenUseCase useCase = newUseCase(true);
        when(refreshTokenStore.find(OLD_REFRESH)).thenReturn(Optional.of(storedRecord()));
        when(userTokenVersionService.getCurrentTokenVersion(USER_ID)).thenReturn(TOKEN_VERSION + 1);

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.refresh(OLD_REFRESH));
        verify(refreshTokenStore).delete(OLD_REFRESH);
        verify(accessTokenPort, never()).generateToken(any(), anyLong(), any(), any());
    }

    @Test
    void refresh_userNotFound_throwsUserNotFound() {
        RefreshAccessTokenUseCase useCase = newUseCase(true);
        when(refreshTokenStore.find(OLD_REFRESH)).thenReturn(Optional.of(storedRecord()));
        when(userTokenVersionService.getCurrentTokenVersion(USER_ID)).thenReturn(TOKEN_VERSION);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> useCase.refresh(OLD_REFRESH)
        );
        assertEquals("NOT_FOUND", ex.getCode());
        verify(accessTokenPort, never()).generateToken(any(), anyLong(), any(), any());
    }

    @Test
    void refresh_tokenVersionServiceUserMissing_throwsUserNotFoundAndDeletes() {
        RefreshAccessTokenUseCase useCase = newUseCase(true);
        when(refreshTokenStore.find(OLD_REFRESH)).thenReturn(Optional.of(storedRecord()));
        when(userTokenVersionService.getCurrentTokenVersion(USER_ID))
                .thenThrow(new ResourceNotFoundException("USER_NOT_FOUND", "gone"));

        assertThrows(UserNotFoundException.class, () -> useCase.refresh(OLD_REFRESH));
        verify(refreshTokenStore).delete(OLD_REFRESH);
    }

    private RefreshAccessTokenUseCase newUseCase(boolean rotate) {
        return new RefreshAccessTokenUseCase(
                refreshTokenStore,
                userTokenVersionService,
                userRepository,
                accessTokenPort,
                rotate
        );
    }

    private static RefreshTokenRecord storedRecord() {
        return new RefreshTokenRecord(
                OLD_REFRESH,
                USER_ID,
                TOKEN_VERSION,
                Instant.parse("2026-08-01T00:00:00Z")
        );
    }

    private static User sampleUser() {
        UUID roleId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        return User.reconstitute(
                USER_ID, "c1", "1", "CC", "Admin", "admin@test.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                true, TOKEN_VERSION, List.of(roleId), List.of("Administrador"), List.of("ADMINISTRADOR"),
                List.of("dashboard.view"), false, null, null, 0, null, null
        );
    }
}
