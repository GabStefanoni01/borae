package com.borae.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlbFileValidatorTest {

    private final GlbFileValidator validator = new GlbFileValidator(new ObjectMapper());

    @Test
    void acceptsStructurallyValidGlb2File() {
        assertDoesNotThrow(() -> validator.validate(validGlb()));
    }

    @Test
    void rejectsWrongMagicVersionAndDeclaredLength() {
        byte[] bytes = validGlb();
        bytes[0] = 0;
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> validator.validate(bytes)).getStatusCode().value());

        bytes = validGlb();
        bytes[4] = 1;
        byte[] wrongVersion = bytes;
        assertThrows(ResponseStatusException.class, () -> validator.validate(wrongVersion));

        bytes = validGlb();
        bytes[8] = 0;
        byte[] wrongLength = bytes;
        assertThrows(ResponseStatusException.class, () -> validator.validate(wrongLength));
    }

    @Test
    void rejectsInvalidJsonChunkAndTruncatedChunk() {
        byte[] bytes = validGlb();
        bytes[20] = (byte) '{';
        bytes[21] = (byte) 'x';
        bytes[22] = (byte) '}';
        byte[] invalidJson = bytes;
        assertThrows(ResponseStatusException.class, () -> validator.validate(invalidJson));

        byte[] truncated = new byte[15];
        assertThrows(ResponseStatusException.class, () -> validator.validate(truncated));
    }

    private byte[] validGlb() {
        byte[] json = "{\"asset\":{\"version\":\"2.0\"}}".getBytes(StandardCharsets.UTF_8);
        int paddedLength = (json.length + 3) & ~3;
        ByteBuffer buffer = ByteBuffer.allocate(12 + 8 + paddedLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x46546C67);
        buffer.putInt(2);
        buffer.putInt(buffer.capacity());
        buffer.putInt(paddedLength);
        buffer.putInt(0x4E4F534A);
        buffer.put(json);
        while (buffer.position() < buffer.capacity()) {
            buffer.put((byte) 0x20);
        }
        return buffer.array();
    }
}
