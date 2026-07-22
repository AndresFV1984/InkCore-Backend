package com.inkcore.application.client.usecase;

import com.inkcore.domain.client.exception.ClientAlreadyExistsException;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateClientUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-22T12:00:00Z");

    @Mock ClientRepositoryPort clientRepository;

    private CreateClientUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateClientUseCase(
                clientRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsStateTrue() {
        when(clientRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                "company-seed-001", "900123456-1")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client created = useCase.execute(new CreateClientCommand(
                "company-seed-001",
                "Comercializadora ABC S.A.S.",
                "nit",
                "900123456-1",
                "Antioquia",
                "Medellín",
                "Calle 10 # 20-30",
                "604 123 4567",
                "correo@empresa.com",
                "Ana Gómez",
                null
        ));

        assertEquals("Comercializadora ABC S.A.S.", created.getName());
        assertEquals("NIT", created.getDocumentType());
        assertEquals("900123456-1", created.getIdentification());
        assertEquals("Antioquia", created.getDepartment());
        assertEquals("Medellín", created.getCity());
        assertEquals("correo@empresa.com", created.getEmail());
        assertEquals("Ana Gómez", created.getContactPerson());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 7, 22), created.getCreationDate());

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertEquals("company-seed-001", captor.getValue().getCompanyId());
    }

    @Test
    void execute_withoutIdentification_skipsDuplicateCheck() {
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client created = useCase.execute(new CreateClientCommand(
                "company-seed-001",
                "Cliente Sin NIT",
                null,
                null,
                "Antioquia",
                "Medellín",
                null,
                null,
                null,
                null,
                true
        ));

        assertEquals("Cliente Sin NIT", created.getName());
        verify(clientRepository, never()).existsByCompanyIdAndIdentificationIgnoreCase(any(), any());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void execute_duplicateIdentification_throwsConflict() {
        when(clientRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                "company-seed-001", "900123456-1")).thenReturn(true);

        ClientAlreadyExistsException ex = assertThrows(
                ClientAlreadyExistsException.class,
                () -> useCase.execute(new CreateClientCommand(
                        "company-seed-001",
                        "Otro Cliente",
                        "NIT",
                        "900123456-1",
                        "Antioquia",
                        "Medellín",
                        null,
                        null,
                        null,
                        null,
                        true
                ))
        );

        assertEquals("identification", ex.getField());
        assertEquals("CONFLICT", ex.getCode());
        verify(clientRepository, never()).save(any());
    }
}
