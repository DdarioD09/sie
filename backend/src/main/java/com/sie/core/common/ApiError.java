package com.sie.core.common;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned for every failed request, so the Angular frontend can rely on one
 * shape regardless of which endpoint failed.
 */
public record ApiError(
    Instant timestamp, int status, String error, String message, String path, List<String> details) {}
