package com.borae.catalog;

import java.util.UUID;

public record MenuResponse(
        UUID id,
        String name,
        String description,
        MenuStatus status,
        int displayOrder
) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getName(), menu.getDescription(),
                menu.getStatus(), menu.getDisplayOrder());
    }
}
