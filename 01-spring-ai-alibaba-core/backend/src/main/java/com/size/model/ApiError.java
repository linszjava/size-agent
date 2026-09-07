package com.size.model;

import java.time.Instant;

public record ApiError(
        String requestId,
        String code,
        String message,
        Instant timestamp
) {
}
