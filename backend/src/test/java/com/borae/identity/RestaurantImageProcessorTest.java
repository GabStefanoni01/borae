package com.borae.identity;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestaurantImageProcessorTest {

    private final RestaurantImageProcessor processor = new RestaurantImageProcessor();

    @Test
    void resizesLargePngWithoutChangingItsAspectRatio() throws Exception {
        BufferedImage image = new BufferedImage(3000, 1500, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        ImageIO.write(image, "png", source);

        RestaurantImageProcessor.ProcessedImage result =
                processor.process(source.toByteArray(), "cover.png", "image/png");

        assertEquals(2000, result.width());
        assertEquals(1000, result.height());
        assertEquals("image/png", result.contentType());
        BufferedImage optimized = ImageIO.read(new java.io.ByteArrayInputStream(result.bytes()));
        assertEquals(2000, optimized.getWidth());
        assertEquals(1000, optimized.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, optimized.getType());
    }

    @Test
    void rejectsMismatchBetweenFileExtensionMimeTypeAndActualImage() throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        ImageIO.write(image, "png", source);

        assertThrows(ResponseStatusException.class,
                () -> processor.process(source.toByteArray(), "photo.jpg", "image/jpeg"));
    }

    @Test
    void rejectsUnsupportedExtensions() {
        assertThrows(ResponseStatusException.class,
                () -> processor.process(new byte[]{1, 2, 3}, "photo.gif", "image/gif"));
    }
}
