package com.borae.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class GlbFileValidator {

    private static final int HEADER_SIZE = 12;
    private static final int CHUNK_HEADER_SIZE = 8;
    private static final int JSON_CHUNK = 0x4E4F534A;
    private static final int BIN_CHUNK = 0x004E4942;

    private final ObjectMapper objectMapper;

    public GlbFileValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(byte[] bytes) {
        if (bytes == null || bytes.length < HEADER_SIZE + CHUNK_HEADER_SIZE) {
            throw invalid();
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.getInt(0) != 0x46546C67 || buffer.getInt(4) != 2 || buffer.getInt(8) != bytes.length) {
            throw invalid();
        }

        int offset = HEADER_SIZE;
        int chunkIndex = 0;
        int binChunkLength = -1;
        JsonNode root = null;

        while (offset < bytes.length) {
            if (bytes.length - offset < CHUNK_HEADER_SIZE) {
                throw invalid();
            }

            int chunkLength = buffer.getInt(offset);
            int chunkType = buffer.getInt(offset + 4);
            offset += CHUNK_HEADER_SIZE;

            if (chunkLength < 0 || chunkLength % 4 != 0 || chunkLength > bytes.length - offset) {
                throw invalid();
            }

            chunkIndex++;
            if (chunkIndex == 1 && chunkType != JSON_CHUNK) {
                throw invalid();
            }

            if (chunkType == JSON_CHUNK) {
                if (chunkIndex != 1 || root != null) {
                    throw invalid();
                }
                byte[] jsonBytes = new byte[chunkLength];
                System.arraycopy(bytes, offset, jsonBytes, 0, chunkLength);
                try {
                    root = objectMapper.readTree(new String(jsonBytes, StandardCharsets.UTF_8).strip());
                    if (root == null || !root.isObject() || !root.path("asset").isObject()
                            || !"2.0".equals(root.path("asset").path("version").asText())) {
                        throw invalid();
                    }
                    rejectExternalResources(root);
                } catch (ResponseStatusException exception) {
                    throw exception;
                } catch (Exception exception) {
                    throw invalid();
                }
            } else if (chunkType == BIN_CHUNK) {
                // GLB permits at most one BIN chunk, immediately after the JSON chunk.
                if (chunkIndex != 2 || binChunkLength >= 0) {
                    throw invalid();
                }
                binChunkLength = chunkLength;
            } else {
                // Reject arbitrary unknown chunks to prevent hidden payloads
                throw invalid();
            }

            offset += chunkLength;
        }

        if (root == null || offset != bytes.length) {
            throw invalid();
        }
        validateBinaryBuffer(root, binChunkLength);
    }

    private void validateBinaryBuffer(JsonNode root, int binChunkLength) {
        JsonNode buffers = root.path("buffers");
        if (!buffers.isMissingNode() && !buffers.isArray()) {
            throw invalid();
        }
        if (buffers.isArray()) {
            // A GLB contains at most one embedded buffer, stored in its BIN chunk.
            if (buffers.size() > 1) {
                throw invalid();
            }
            if (buffers.size() == 1) {
                JsonNode buffer = buffers.get(0);
                JsonNode byteLength = buffer.path("byteLength");
                if (!buffer.isObject() || !byteLength.isIntegralNumber()
                        || byteLength.asLong() <= 0 || binChunkLength < 0) {
                    throw invalid();
                }
                long declaredLength = byteLength.asLong();
                if (binChunkLength < declaredLength || binChunkLength - declaredLength > 3) {
                    throw invalid();
                }
            } else if (binChunkLength >= 0) {
                throw invalid();
            }
        } else if (binChunkLength >= 0) {
            throw invalid();
        }
    }

    private void rejectExternalResources(JsonNode root) {
        JsonNode buffers = root.path("buffers");
        if (buffers.isArray()) {
            for (JsonNode item : buffers) {
                // A GLB buffer must be embedded in its BIN chunk, not fetched from a remote URL.
                if (!item.isObject() || item.has("uri")) {
                    throw invalid();
                }
            }
        }

        JsonNode images = root.path("images");
        if (images.isArray()) {
            for (JsonNode item : images) {
                if (!item.isObject()) {
                    throw invalid();
                }
                JsonNode uriNode = item.get("uri");
                // Only safe embedded raster image data URIs are allowed; remote, relative, SVG, or HTML data URIs are rejected.
                if (uriNode != null) {
                    if (!uriNode.isTextual()) {
                        throw invalid();
                    }
                    String uri = uriNode.asText().trim().toLowerCase(Locale.ROOT);
                    if (!isAllowedImageDataUri(uri)) {
                        throw invalid();
                    }
                }
            }
        } else if (!images.isMissingNode()) {
            throw invalid();
        }
    }

    private boolean isAllowedImageDataUri(String lowerUri) {
        return lowerUri.startsWith("data:image/png;")
                || lowerUri.startsWith("data:image/png,")
                || lowerUri.startsWith("data:image/jpeg;")
                || lowerUri.startsWith("data:image/jpeg,")
                || lowerUri.startsWith("data:image/jpg;")
                || lowerUri.startsWith("data:image/jpg,")
                || lowerUri.startsWith("data:image/webp;")
                || lowerUri.startsWith("data:image/webp,");
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "O arquivo enviado não é um GLB 2.0 válido ou contém referências externas não permitidas.");
    }
}
