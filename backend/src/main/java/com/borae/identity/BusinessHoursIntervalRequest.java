package com.borae.identity;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BusinessHoursIntervalRequest(
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt
) {
}
