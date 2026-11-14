package com.borae.identity;

import java.time.LocalTime;

public record BusinessHoursIntervalResponse(
        LocalTime opensAt,
        LocalTime closesAt
) {
}
