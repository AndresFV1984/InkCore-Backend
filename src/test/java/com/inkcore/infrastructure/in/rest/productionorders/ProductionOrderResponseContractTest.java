package com.inkcore.infrastructure.in.rest.productionorders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato GET: nombres camelCase que consume el front al rehidratar el wizard.
 */
class ProductionOrderResponseContractTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void from_serializesAggregateIdsWithFrontendContractNames() throws Exception {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("order-1");
        order.setCompanyId("company-1");
        order.setOrderNumber("OP-1");
        order.setVersion(5L);
        order.setClientId("client-1");
        order.setWorkName("Flyer");
        order.setRequestedQuantity(1000);

        Plate plate = new Plate();
        plate.setPlateId("plate-srv-1");
        plate.setColors("4 COLORES");
        plate.setQuantity(1000);
        order.setPlates(List.of(plate));

        PaperRow row = new PaperRow();
        row.setPaperRowId("pr-1");
        row.setPlateId("plate-srv-1");
        row.setCutRowKey("plate-srv-1");
        row.setClientSuppliesPaper(true);
        row.setPaperCut(true);
        row.setCutLayoutId("corte-1");
        row.setPiecesPerSheet(4);
        order.setPaperRows(List.of(row));

        PrintConfig print = new PrintConfig();
        print.setPrintId("print-1");
        print.setPlateId("plate-srv-1");
        print.setCompleted(true);
        order.setPrints(List.of(print));

        PostpressRecord finished = new PostpressRecord();
        finished.setRecordId("pp-term-1");
        finished.setPlateId("plate-srv-1");
        finished.setType(PostpressType.FINISHED_PRODUCT);
        finished.setCompleted(true);
        PostpressLine line = new PostpressLine();
        line.setLineId("line-t-1");
        line.setCatalogItemId("term-1");
        line.setItemName("Barniz");
        line.setSource("catalog");
        line.setValuePerCm2(new BigDecimal("10"));
        finished.setLines(List.of(line));
        order.setPostpressRecords(List.of(finished));

        JsonNode data = mapper.valueToTree(ProductionOrderResponse.from(order));

        assertEquals("plate-srv-1", data.path("plates").get(0).path("productionOrderPlateId").asText());
        assertTrue(data.path("plates").get(0).path("plateId").isMissingNode());

        assertEquals("pr-1", data.path("paperRows").get(0).path("productionOrderPaperRowId").asText());
        assertEquals("plate-srv-1", data.path("paperRows").get(0).path("plateId").asText());
        assertEquals("corte-1", data.path("paperRows").get(0).path("cutLayoutId").asText());
        assertEquals(4, data.path("paperRows").get(0).path("piecesPerSheet").asInt());

        assertEquals("print-1", data.path("prints").get(0).path("productionOrderPrintId").asText());
        assertEquals("plate-srv-1", data.path("prints").get(0).path("plateId").asText());

        JsonNode postpress = data.path("postpressRecords").get(0);
        assertEquals("pp-term-1", postpress.path("productionOrderPostpressRecordId").asText());
        assertEquals("FINISHED_PRODUCT", postpress.path("type").asText());
        assertEquals("line-t-1", postpress.path("lines").get(0).path("productionOrderPostpressLineId").asText());
    }
}
