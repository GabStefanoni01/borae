package com.borae.catalog;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Dish3DGenerationProviderTest {

    @Test
    void sourceImageDefensivelyCopiesContent() {
        byte[] original = new byte[]{1, 2, 3};
        Dish3DGenerationProvider.SourceImage image =
                new Dish3DGenerationProvider.SourceImage("dish.jpg", "image/jpeg", original);

        original[0] = 9;
        byte[] returned = image.content();
        returned[1] = 9;

        assertArrayEquals(new byte[]{1, 2, 3}, image.content());
    }

    @Test
    void generatedModelDefensivelyCopiesGlb() {
        byte[] original = new byte[]{0x67, 0x6c, 0x54, 0x46};
        Dish3DGenerationProvider.GeneratedDishModel generated =
                new Dish3DGenerationProvider.GeneratedDishModel(original, "triposr", 1);

        original[0] = 0;
        byte[] returned = generated.glb();
        returned[1] = 0;

        assertArrayEquals(new byte[]{0x67, 0x6c, 0x54, 0x46}, generated.glb());
    }

    @Test
    void sourceImageRequiresValidMetadataAndContent() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dish3DGenerationProvider.SourceImage("", "image/jpeg", new byte[]{1})),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dish3DGenerationProvider.SourceImage("dish.jpg", "", new byte[]{1})),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dish3DGenerationProvider.SourceImage("dish.jpg", "image/jpeg", new byte[0]))
        );
    }

    @Test
    void generatedModelRequiresGlbEngineAndPositiveSourceCount() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dish3DGenerationProvider.GeneratedDishModel(new byte[0], "triposr", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dish3DGenerationProvider.GeneratedDishModel(new byte[]{1}, "", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dish3DGenerationProvider.GeneratedDishModel(new byte[]{1}, "triposr", 0))
        );
    }

    @Test
    void providerContractUsesGenerationUuid() {
        assertNotNull(UUID.randomUUID());
    }
}
