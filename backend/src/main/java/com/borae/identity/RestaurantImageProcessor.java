package com.borae.identity;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Component
public class RestaurantImageProcessor {

    private static final int MAX_EDGE = 2000;
    private static final int MAX_SOURCE_DIMENSION = 12_000;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final float QUALITY = 0.85f;

    public ProcessedImage process(byte[] source, String filename, String declaredContentType) {
        if (source == null || source.length < 12) {
            throw badImage();
        }
        String extension = extension(filename);
        String expectedType = switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw badImage();
        };
        if (declaredContentType == null || !expectedType.equalsIgnoreCase(declaredContentType.trim())) {
            throw badImage();
        }
        if (!matchesMagicBytes(source, expectedType)) {
            throw badImage();
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(source);
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) throw badImage();
            Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw badImage();
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String actualFormat = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean formatMatches = switch (expectedType) {
                    case "image/jpeg" -> actualFormat.equals("jpeg") || actualFormat.equals("jpg");
                    case "image/png" -> actualFormat.equals("png");
                    case "image/webp" -> actualFormat.equals("webp");
                    default -> false;
                };
                if (!formatMatches) throw badImage();
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1
                        || width > MAX_SOURCE_DIMENSION
                        || height > MAX_SOURCE_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw badImage();
                }
                BufferedImage original = reader.read(0);
                if (original == null) throw badImage();
                BufferedImage oriented = applyExifOrientation(original, exifOrientation(source));
                int orientedWidth = oriented.getWidth();
                int orientedHeight = oriented.getHeight();

                double scale = Math.min(1d, (double) MAX_EDGE / Math.max(orientedWidth, orientedHeight));
                int targetWidth = Math.max(1, (int) Math.round(orientedWidth * scale));
                int targetHeight = Math.max(1, (int) Math.round(orientedHeight * scale));
                int imageType = oriented.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
                BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
                Graphics2D graphics = resized.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(oriented, 0, 0, targetWidth, targetHeight, null);
                graphics.dispose();

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                String writerFormat = expectedType.equals("image/jpeg") ? "jpeg" :
                        expectedType.equals("image/png") ? "png" : "webp";
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writerFormat);
                if (!writers.hasNext()) throw new IllegalStateException("Image writer unavailable: " + writerFormat);
                ImageWriter writer = writers.next();
                try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
                    writer.setOutput(imageOutput);
                    ImageWriteParam params = writer.getDefaultWriteParam();
                    if (params.canWriteCompressed()) {
                        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        params.setCompressionQuality(QUALITY);
                    }
                    writer.write(null, new IIOImage(resized, null, null), params);
                } finally {
                    writer.dispose();
                    original.flush();
                    if (oriented != original) oriented.flush();
                    resized.flush();
                }
                return new ProcessedImage(output.toByteArray(), expectedType, extension, targetWidth, targetHeight);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ResponseStatusException responseStatusException) throw responseStatusException;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível processar a imagem enviada.");
        }
    }

    private boolean matchesMagicBytes(byte[] bytes, String expectedType) {
        return switch (expectedType) {
            case "image/jpeg" -> (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "image/webp" -> bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
            default -> false;
        };
    }

    private int exifOrientation(byte[] source) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(source));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            return directory == null || !directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)
                    ? 1
                    : directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (ImageProcessingException | IOException | RuntimeException | com.drew.metadata.MetadataException ignored) {
            return 1;
        }
    }

    private BufferedImage applyExifOrientation(BufferedImage source, int orientation) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean swapDimensions = orientation >= 5 && orientation <= 8;
        int targetWidth = swapDimensions ? height : width;
        int targetHeight = swapDimensions ? width : height;
        AffineTransform transform = new AffineTransform();
        switch (orientation) {
            case 2 -> transform.setTransform(-1, 0, 0, 1, width, 0);
            case 3 -> transform.setTransform(-1, 0, 0, -1, width, height);
            case 4 -> transform.setTransform(1, 0, 0, -1, 0, height);
            case 5 -> transform.setTransform(0, 1, 1, 0, 0, 0);
            case 6 -> transform.setTransform(0, 1, -1, 0, height, 0);
            case 7 -> transform.setTransform(0, -1, -1, 0, height, width);
            case 8 -> transform.setTransform(0, -1, 1, 0, 0, width);
            default -> { return source; }
        }
        int imageType = source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage oriented = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = oriented.createGraphics();
        graphics.drawImage(source, transform, null);
        graphics.dispose();
        return oriented;
    }

    private String extension(String filename) {
        if (filename == null || filename.indexOf('\0') >= 0 || !filename.contains(".")) {
            throw badImage();
        }
        String sanitized = filename.trim();
        if (sanitized.contains("..") || sanitized.contains("/") || sanitized.contains("\\")) {
            throw badImage();
        }
        return sanitized.substring(sanitized.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException badImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Envie uma imagem JPEG, PNG ou WebP válida, com extensão e MIME type compatíveis.");
    }

    public record ProcessedImage(byte[] bytes, String contentType, String extension, int width, int height) {}
}
