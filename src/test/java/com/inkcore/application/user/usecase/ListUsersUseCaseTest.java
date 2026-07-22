package com.inkcore.application.user.usecase;

import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    UserRepositoryPort userRepository;

    private ListUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUsersUseCase(userRepository);
    }

    @Test
    void execute_whenStateNull_callsFindAll() {
        List<User> all = List.of(sampleUser(true), sampleUser(false));
        when(userRepository.findAll()).thenReturn(all);

        List<User> result = useCase.execute(null);

        assertSame(all, result);
        verify(userRepository).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void execute_whenStateTrue_callsFindAllByStateTrue() {
        List<User> active = List.of(sampleUser(true));
        when(userRepository.findAllByState(true)).thenReturn(active);

        List<User> result = useCase.execute(true);

        assertEquals(1, result.size());
        assertSame(active, result);
        verify(userRepository).findAllByState(true);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void execute_whenStateFalse_callsFindAllByStateFalse() {
        List<User> inactive = List.of(sampleUser(false));
        when(userRepository.findAllByState(false)).thenReturn(inactive);

        List<User> result = useCase.execute(false);

        assertEquals(1, result.size());
        assertSame(inactive, result);
        verify(userRepository).findAllByState(false);
        verifyNoMoreInteractions(userRepository);
    }

    private static User sampleUser(boolean state) {
        UUID roleId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        return User.reconstitute(
                "u1", "company-seed-001", "1", "CC", "User", "user@test.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                state, 1L, List.of(roleId), List.of("Administrador"), List.of("ADMINISTRADOR"),
                List.of("dashboard.view"), false, null, null, 0, null, null
        );
    }
}
