package com.borae.catalog;

import java.util.List;
import java.util.UUID;

/**
 * Provider boundary for generating a GLB from restaurant-owned dish photos.
 *
 * The catalog domain must not depend on Hugging Face, Gradio, TripoSR or any
 * other engine implementation. Implementations are responsible for translating
 * this contract to the selected engine and returning the generated artifact.
 */
public interface Dish3DGenerationProvider {

    GeneratedDishModel generate(UUID generationId, List<SourceImage> sourceImages);

    record SourceImage(
            String fileName,
            String contentType,
            byte[] content
    ) {
        public SourceImage {
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("fileName is required");
            }
            if (contentType == null || contentType.isBlank()) {
                throw new IllegalArgumentException("contentType is required");
            }
            if (content == null || content.length == 0) {
                throw new IllegalArgumentException("image content is required");
            }
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    record GeneratedDishModel(
            byte[] glb,
            String engine,
            int sourceImageCount
    ) {
        public GeneratedDishModel {
            if (glb == null || glb.length == 0) {
                throw new IllegalArgumentException("generated GLB is required");
            }
            if (engine == null || engine.isBlank()) {
                throw new IllegalArgumentException("engine is required");
            }
            if (sourceImageCount < 1) {
                throw new IllegalArgumentException("sourceImageCount must be positive");
            }
            glb = glb.clone();
        }

        @Override
        public byte[] glb() {
            return glb.clone();
        }
    }
}
