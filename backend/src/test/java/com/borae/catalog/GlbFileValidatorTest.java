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
        assertDoesNotThrow(() -> validator.validate(validGlb("{\"asset\":{\"version\":\"2.0\"}}")));
    }

    @Test
    void rejectsWrongMagicVersionAndDeclaredLength() {
        byte[] wrongMagic = validGlb("{\"asset\":{\"version\":\"2.0\"}}");
        wrongMagic[0] = 0;
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> validator.validate(wrongMagic)).getStatusCode().value());

        byte[] wrongVersion = validGlb("{\"asset\":{\"version\":\"2.0\"}}");
        wrongVersion[4] = 1;
        assertThrows(ResponseStatusException.class, () -> validator.validate(wrongVersion));

        byte[] wrongLength = validGlb("{\"asset\":{\"version\":\"2.0\"}}");
        wrongLength[8] = 0;
        assertThrows(ResponseStatusException.class, () -> validator.validate(wrongLength));
    }

    @Test
    void rejectsInvalidJsonChunkAndTruncatedChunk() {
        byte[] bytes = validGlb("{\"asset\":{\"version\":\"2.0\"}}");
        bytes[20] = (byte) '{';
        bytes[21] = (byte) 'x';
        bytes[22] = (byte) '}';
        assertThrows(ResponseStatusException.class, () -> validator.validate(bytes));

        byte[] truncated = new byte[15];
        assertThrows(ResponseStatusException.class, () -> validator.validate(truncated));
    }

    @Test
    void rejectsExternalBufferAndImageReferences() {
        byte[] externalBuffer = validGlb(
                "{\"asset\":{\"version\":\"2.0\"},\"buffers\":[{\"uri\":\"https://example.com/model.bin\",\"byteLength\":4}]}");
        assertThrows(ResponseStatusException.class, () -> validator.validate(externalBuffer));

        byte[] externalImage = validGlb(
                "{\"asset\":{\"version\":\"2.0\"},\"images\":[{\"uri\":\"//example.com/image.png\"}]}");
        assertThrows(ResponseStatusException.class, () -> validator.validate(externalImage));
    }

    @Test
    void acceptsEmbeddedDataUriImageReference() {
        byte[] embeddedImage = validGlb(
                "{\"asset\":{\"version\":\"2.0\"},\"images\":[{\"uri\":\"data:image/png;base64,AA==\"}]}");
        assertDoesNotThrow(() -> validator.validate(embeddedImage));
    }

    @Test
    void requiresAndValidatesBinChunkForEmbeddedBuffer() {
        String json = "{\"asset\":{\"version\":\"2.0\"},\"buffers\":[{\"byteLength\":4}]}";

        assertThrows(ResponseStatusException.class,
                () -> validator.validate(validGlb(json)));
        assertDoesNotThrow(() -> validator.validate(glbWithBin(json, new byte[4])));
        assertThrows(ResponseStatusException.class,
                () -> validator.validate(glbWithBin(json, new byte[8])));
    }

    private byte[] validGlb(String jsonText) {
        return glbWithBin(jsonText, null);
    }

    private byte[] glbWithBin(String jsonText, byte[] binBytes) {
        byte[] json = jsonText.getBytes(StandardCharsets.UTF_8);
        int paddedJsonLength = (json.length + 3) & ~3;
        int paddedBinLength = binBytes == null ? 0 : (binBytes.length + 3) & ~3;
        int binChunkSize = binBytes == null ? 0 : 8 + paddedBinLength;
        ByteBuffer buffer = ByteBuffer.allocate(12 + 8 + paddedJsonLength + binChunkSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x46546C67);
        buffer.putInt(2);
        buffer.putInt(buffer.capacity());
        buffer.putInt(paddedJsonLength);
        buffer.putInt(0x4E4F534A);
        buffer.put(json);
        while (buffer.position() < 20 + paddedJsonLength) {
            buffer.put((byte) 0x20);
        }
        if (binBytes != null) {
            buffer.putInt(paddedBinLength);
            buffer.putInt(0x004E4942);
            buffer.put(binBytes);
            while (buffer.hasRemaining()) {
                buffer.put((byte) 0);
            }
        }
        return buffer.array();
    }
}
