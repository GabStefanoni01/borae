package com.borae.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/restaurants")
@Tag(name = "Cardápio público", description = "Consulta pública de restaurantes e cardápios publicados")
public class PublicRestaurantMenuController {

    private final PublicRestaurantMenuService publicRestaurantMenuService;

    public PublicRestaurantMenuController(PublicRestaurantMenuService publicRestaurantMenuService) {
        this.publicRestaurantMenuService = publicRestaurantMenuService;
    }

    @GetMapping("/{slug}/menu")
    @Operation(
            summary = "Consultar cardápios públicos por slug",
            description = "Retorna somente restaurantes publicados e ativos, cardápios publicados, categorias ativas e produtos não arquivados."
    )
    public PublicRestaurantMenuResponse getBySlug(@PathVariable String slug) {
        return publicRestaurantMenuService.getBySlug(slug);
    }
}
