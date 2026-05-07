package com.pos.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

public record ErrorResponse(String error_code, String message, Instant timestamp) {}
