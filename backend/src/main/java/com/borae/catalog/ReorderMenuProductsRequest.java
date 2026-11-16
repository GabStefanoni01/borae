package com.borae.catalog;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderMenuProductsRequest(
        @NotNull UUID categoryId,
        @NotEmpty List<@NotNull UUID> productIds
) {
}
