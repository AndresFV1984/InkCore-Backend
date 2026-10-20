package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "OrderNegotiatedAdditionListSuccessEnvelope")
public record OrderNegotiatedAdditionListSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        @Schema(implementation = OrderResponses.NegotiatedAdditionResponse.class)
        List<OrderResponses.NegotiatedAdditionResponse> data
) {
}
