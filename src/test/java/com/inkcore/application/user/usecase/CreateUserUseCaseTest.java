package com.inkcore.application.user.usecase;

import com.inkcore.application.shared.PasswordHasherPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.exception.UserAlreadyExistsException;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-18T12:00:00Z");
    private static final UUID ROLE_ID = UUID.fromString("b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22");

    @Mock UserRepositoryPort userRepository;
    @Mock RoleRepositoryPort roleRepository;
    @Mock PasswordHasherPort passwordHasher;

    private PasswordPolicyProperties passwordPolicy;
    private CreateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        passwordPolicy = new PasswordPolicyProperties();
        passwordPolicy.setExpirationDays(90);
        useCase = new CreateUserUseCase(
                userRepository,
                roleRepository,
                passwordHasher,
                passwordPolicy,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void create_success_withRole() {
        when(userRepository.existsByMailIgnoreCase("ana@itm.edu.co")).thenReturn(false);
        when(userRepository.existsByIdentificationNumber("1234567890")).thenReturn(false);
        when(roleRepository.findByRoleName("Administrador")).thenReturn(Optional.of(
                Role.reconstitute(ROLE_ID, "c1", "Administrador", "", true, List.of("dashboard.view"))
        ));
        when(passwordHasher.hash("SecurePass123*")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = useCase.create(
                "company-seed-001", "1234567890", "CC", "Ana García", "ana@itm.edu.co",
                "3001234567", "Antioquia", "Medellín", "Calle 10",
                "SecurePass123*", "Administrador", true
        );

        assertEquals(0L, created.getTokenVersion());
        assertTrue(Boolean.TRUE.equals(created.getForcePasswordChange()));
        assertEquals(0, created.getFailedAttempts());
        assertNull(created.getLastLoginAt());
        assertEquals("bcrypt-hash", created.getPasswordHash());
        assertEquals("CC", created.getDocumentType());
        assertEquals("Antioquia", created.getDepartment());
        assertEquals("Medellín", created.getCity());
        assertEquals(List.of(ROLE_ID), created.getRoleIds());
        assertEquals(List.of("Administrador"), created.getRoleNames());
        assertEquals(List.of("ADMINISTRADOR"), created.getRoleCodes());
        assertEquals(List.of("dashboard.view"), created.getPermissionCodes());
        assertEquals(
                LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC).plusDays(90),
                created.getPasswordExpiresAt()
        );
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_unknownRole_throwsNotFound() {
        when(userRepository.existsByMailIgnoreCase("ana@itm.edu.co")).thenReturn(false);
        when(userRepository.existsByIdentificationNumber("1234567890")).thenReturn(false);
        when(roleRepository.findByRoleName("NO_EXISTE")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> useCase.create(
                        "company-seed-001", "1234567890", "CC", "Ana García", "ana@itm.edu.co",
                        "3001234567", "Antioquia", "Medellín", null,
                        "SecurePass123*", "NO_EXISTE", true
                )
        );
        assertEquals("ROLE_NOT_FOUND", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateMail_throwsConflict() {
        when(userRepository.existsByMailIgnoreCase("ana@itm.edu.co")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> useCase.create(
                        "company-seed-001", "1234567890", "CC", "Ana", "ana@itm.edu.co",
                        "300", "Antioquia", "Medellín", null,
                        "SecurePass123*", "Administrador", true
                )
        );
        assertEquals("mail", ex.getField());
        assertEquals("CONFLICT", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateDocument_throwsConflict() {
        when(userRepository.existsByMailIgnoreCase("ana@itm.edu.co")).thenReturn(false);
        when(userRepository.existsByIdentificationNumber("1234567890")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> useCase.create(
                        "company-seed-001", "1234567890", "CC", "Ana", "ana@itm.edu.co",
                        "300", "Antioquia", "Medellín", null,
                        "SecurePass123*", "Administrador", true
                )
        );
        assertEquals("identificationNumber", ex.getField());
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_nullState_defaultsToTrue() {
        when(userRepository.existsByMailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByIdentificationNumber(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName(anyString())).thenReturn(Optional.of(
                Role.reconstitute(ROLE_ID, "c1", "Operador", "", true, List.of("orders.view"))
        ));
        when(passwordHasher.hash(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = useCase.create(
                "company-seed-001", "1234567890", "CC", "Ana", "ana@itm.edu.co",
                "300", "Antioquia", "Medellín", null,
                "SecurePass123*", "Operador", null
        );

        assertTrue(created.isState());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().isState());
    }
}
