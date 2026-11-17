package com.borae.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@Component
public class GlbFileValidator {

    private static final int HEADER_SIZE = 12;
    private static final int CHUNK_HEADER_SIZE = 8;
    private static final int JSON_CHUNK = 0x4E4F534A;

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
        boolean jsonChunkFound = false;
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

            if (!jsonChunkFound) {
                if (chunkType != JSON_CHUNK) {
                    throw invalid();
                }
                byte[] jsonBytes = new byte[chunkLength];
                System.arraycopy(bytes, offset, jsonBytes, 0, chunkLength);
                try {
                    JsonNode root = objectMapper.readTree(new String(jsonBytes, StandardCharsets.UTF_8).strip());
                    if (root == null || !"2.0".equals(root.path("asset").path("version").asText())) {
                        throw invalid();
                    }
                    rejectExternalResources(root);
                } catch (ResponseStatusException exception) {
                    throw exception;
                } catch (Exception exception) {
                    throw invalid();
                }
                jsonChunkFound = true;
            }

            offset += chunkLength;
        }

        if (!jsonChunkFound || offset != bytes.length) {
            throw invalid();
        }
    }

    private void rejectExternalResources(JsonNode root) {
        JsonNode buffers = root.path("buffers");
        if (buffers.isArray()) {
            for (JsonNode item : buffers) {
                // A GLB buffer must be embedded in its BIN chunk, not fetched from a remote URL.
                if (item.hasNonNull("uri")) {
                    throw invalid();
                }
            }
        }

        JsonNode images = root.path("images");
        if (images.isArray()) {
            for (JsonNode item : images) {
                String uri = item.path("uri").asText(null);
                // Embedded data URIs are allowed; remote and relative references are not.
                if (uri != null && !uri.startsWith("data:")) {
                    throw invalid();
                }
            }
        }
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "O arquivo enviado não é um GLB 2.0 válido ou contém referências externas não permitidas.");
    }
}
