package com.wex.exchange.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** The envelope Treasury wraps its rows in. We only care about {@code data}; meta/links are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RatesResponse(List<RateRecord> data) {
}
