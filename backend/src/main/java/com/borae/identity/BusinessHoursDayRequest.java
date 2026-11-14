package com.borae.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;

public record BusinessHoursDayRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotEmpty List<@NotNull @Valid BusinessHoursIntervalRequest> intervals
) {
}
