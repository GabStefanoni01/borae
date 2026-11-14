package com.borae.identity;

import java.time.DayOfWeek;
import java.util.List;

public record BusinessHoursDayResponse(
        DayOfWeek dayOfWeek,
        List<BusinessHoursIntervalResponse> intervals
) {
}
