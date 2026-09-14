package com.size.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    @Test
    void shouldReuseSafeRequestId() {
        assertThat(RequestCorrelationFilter.resolveRequestId("order_2026-09"))
                .isEqualTo("order_2026-09");
    }

    @Test
    void shouldReplaceUnsafeRequestId() {
        String generated = RequestCorrelationFilter.resolveRequestId("bad id\nforged-log");

        assertThat(generated).matches("[a-f0-9]{32}");
    }
}
