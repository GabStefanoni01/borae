package com.borae.catalog;

import jakarta.validation.constraints.NotNull;

public record ChangeProductAvailabilityRequest(@NotNull ProductAvailability availability) {
}
