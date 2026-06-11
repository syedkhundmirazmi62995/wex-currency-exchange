package com.wex.exchange.web.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Writes monetary values as JSON strings in plain (non-scientific) notation, e.g. {@code "20.00"}.
 * Serializing money as a string keeps the exact scale on the wire and avoids a consumer's JSON
 * parser turning it back into a lossy float.
 */
public class MoneyStringSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeString(value.toPlainString());
    }
}
