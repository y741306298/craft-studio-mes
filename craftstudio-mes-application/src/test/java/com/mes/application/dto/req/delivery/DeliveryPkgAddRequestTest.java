package com.mes.application.dto.req.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryPkgAddRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsProductionPieceIdFromCompactRequest() throws Exception {
        DeliveryPkgAddRequest request = objectMapper.readValue("""
                {"pieces":[{"productionPieceId":"PP_NEW","quantity":2}]}
                """, DeliveryPkgAddRequest.class);

        assertEquals("PP_NEW", request.getPieces().get(0).getProductionPieceId());
    }

    @Test
    void readsProductionPieceIdFromLegacyNestedRequest() throws Exception {
        DeliveryPkgAddRequest request = objectMapper.readValue("""
                {"pieces":[{"piece":{"productionPieceId":"PP_LEGACY"},"quantity":2}]}
                """, DeliveryPkgAddRequest.class);

        assertEquals("PP_LEGACY", request.getPieces().get(0).getProductionPieceId());
    }
}
