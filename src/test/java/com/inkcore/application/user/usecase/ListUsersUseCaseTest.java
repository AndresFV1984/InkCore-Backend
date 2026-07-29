package com.inkcore.application.user.usecase;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock UserRepositoryPort userRepository;

    private ListUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUsersUseCase(userRepository);
    }

    @Test
    void execute_nullState_listsAllPaged() {
        PageQuery query = PageQuery.of(0, 20);
        PageResult<User> page = new PageResult<>(List.of(sample()), 0, 20, 1);
        when(userRepository.findPage(query)).thenReturn(page);

        PageResult<User> result = useCase.execute(null, query);

        assertEquals(1, result.content().size());
        verify(userRepository).findPage(query);
    }

    @Test
    void execute_true_listsActivePaged() {
        PageQuery query = PageQuery.of(0, 20);
        PageResult<User> page = new PageResult<>(List.of(sample()), 0, 20, 1);
        when(userRepository.findPageByState(true, query)).thenReturn(page);

        PageResult<User> result = useCase.execute(true, query);

        assertEquals(1, result.totalElements());
        verify(userRepository).findPageByState(true, query);
    }

    @Test
    void execute_false_listsInactivePaged() {
        PageQuery query = PageQuery.of(1, 10);
        PageResult<User> page = new PageResult<>(List.of(), 1, 10, 0);
        when(userRepository.findPageByState(false, query)).thenReturn(page);

        PageResult<User> result = useCase.execute(false, query);

        assertEquals(0, result.content().size());
        verify(userRepository).findPageByState(false, query);
    }

    private static User sample() {
        UUID roleId = UUID.fromString("b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22");
        return User.reconstitute(
                "user-1", "c1", "1", "CC", "Admin", "admin@indicolors.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                true, 1L, List.of(roleId), List.of("Administrador"), List.of("ADMINISTRADOR"),
                List.of("USUARIO_VER"), false, null, null, 0, null, null
        );
    }
}
