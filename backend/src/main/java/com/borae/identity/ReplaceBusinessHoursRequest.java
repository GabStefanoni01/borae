package com.borae.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceBusinessHoursRequest(
        @NotNull List<@NotNull @Valid BusinessHoursDayRequest> days
) {
}
