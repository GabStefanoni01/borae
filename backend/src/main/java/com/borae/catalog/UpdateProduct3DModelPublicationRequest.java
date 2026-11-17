package com.borae.catalog;

import jakarta.validation.constraints.NotNull;

public record UpdateProduct3DModelPublicationRequest(@NotNull Boolean published) {
}
