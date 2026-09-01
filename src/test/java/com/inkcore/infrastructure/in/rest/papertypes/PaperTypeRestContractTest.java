package com.inkcore.infrastructure.in.rest.papertypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeSupplierAssignment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperTypeRestContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void createRequest_rejectsRootSheetValueAndPackageUnit() {
        assertThrows(UnrecognizedPropertyException.class, () -> objectMapper.readValue("""
                {
                  "companyId": "company-seed-001",
                  "name": "Bond 75g",
                  "width": 70.00,
                  "height": 100.00,
                  "sheetValue": 999.00,
                  "packageUnit": 1,
                  "suppliers": [
                    {
                      "supplierId": "sup-1",
                      "sheetValue": 1500.00,
                      "packageUnit": 500
                    }
                  ]
                }
                """, CreatePaperTypeRequest.class));
    }

    @Test
    void response_serializesPricesOnlyInSuppliers() throws Exception {
        PaperType paperType = PaperType.reconstitute(
                "paper-type-1",
                "company-seed-001",
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                false,
                true,
                LocalDate.of(2026, 8, 1),
                List.of(),
                List.of(PaperTypeSupplierAssignment.of("sup-1", new BigDecimal("1500"), 500))
        );

        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(PaperTypeResponse.from(paperType)));

        assertFalse(root.has("sheetValue"));
        assertFalse(root.has("packageUnit"));
        assertTrue(root.has("suppliers"));
        assertEquals(0, new BigDecimal("1500").compareTo(
                root.get("suppliers").get(0).get("sheetValue").decimalValue()));
        assertEquals(500, root.get("suppliers").get(0).get("packageUnit").intValue());
    }
}
