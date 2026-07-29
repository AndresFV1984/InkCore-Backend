package com.inkcore.application.seller.usecase;

import com.inkcore.domain.seller.exception.SellerAlreadyExistsException;
import com.inkcore.domain.seller.model.Seller;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
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
class CreateSellerUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-25T12:00:00Z");

    @Mock SellerRepositoryPort sellerRepository;

    private CreateSellerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateSellerUseCase(
                sellerRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsStateTrue() {
        when(sellerRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                "company-seed-001", "1020304050")).thenReturn(false);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));

        Seller created = useCase.execute(new CreateSellerCommand(
                "company-seed-001",
                "Carlos Andrés Gómez",
                "cc",
                "1020304050",
                "Vendedor@Empresa.com",
                "300 123 4567",
                "Antioquia",
                "Medellín",
                "Calle 10 # 20-30",
                null
        ));

        assertEquals("Carlos Andrés Gómez", created.getFullName());
        assertEquals("CC", created.getDocumentType());
        assertEquals("1020304050", created.getIdentification());
        assertEquals("vendedor@empresa.com", created.getEmail());
        assertEquals("Antioquia", created.getDepartment());
        assertEquals("Medellín", created.getCity());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 7, 25), created.getCreationDate());

        ArgumentCaptor<Seller> captor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository).save(captor.capture());
        assertEquals("company-seed-001", captor.getValue().getCompanyId());
    }

    @Test
    void execute_duplicateIdentification_throwsConflict() {
        when(sellerRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                "company-seed-001", "1020304050")).thenReturn(true);

        assertThrows(SellerAlreadyExistsException.class, () -> useCase.execute(new CreateSellerCommand(
                "company-seed-001",
                "Carlos Andrés Gómez",
                "CC",
                "1020304050",
                "vendedor@empresa.com",
                null,
                "Antioquia",
                "Medellín",
                null,
                true
        )));

        verify(sellerRepository, never()).save(any());
    }
}
