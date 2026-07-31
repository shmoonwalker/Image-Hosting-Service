package com.example.imagehostingservice.image.validation;

import com.example.imagehostingservice.exception.InvalidImageException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageFileValidatorTest {

    private final ImageFileValidator validator =
            new ImageFileValidator();

    @Test
    void shouldAcceptImageWithSafeDimensions() throws IOException {
        MockMultipartFile file = validPng(4_000, 3_000);

        ValidatedImage image = validator.validate(file);

        assertEquals(4_000, image.width());
        assertEquals(3_000, image.height());
    }

    @Test
    void shouldRejectImageWithExcessiveWidth() {
        InvalidImageException exception = assertThrows(
                InvalidImageException.class,
                () -> validator.validate(
                        pngHeader(10_001, 1)
                )
        );

        assertEquals(
                "Image dimensions are too large",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectImageWithExcessiveHeight() {
        InvalidImageException exception = assertThrows(
                InvalidImageException.class,
                () -> validator.validate(
                        pngHeader(1, 10_001)
                )
        );

        assertEquals(
                "Image dimensions are too large",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectImageWithExcessivePixelCount() {
        InvalidImageException exception = assertThrows(
                InvalidImageException.class,
                () -> validator.validate(
                        pngHeader(6_000, 5_000)
                )
        );

        assertEquals(
                "Image dimensions are too large",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectInvalidImageData() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        assertThrows(
                InvalidImageException.class,
                () -> validator.validate(file)
        );
    }

    private MockMultipartFile validPng(
            int width,
            int height
    ) throws IOException {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        ImageIO.write(image, "png", outputStream);

        return new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                outputStream.toByteArray()
        );
    }

    private MockMultipartFile pngHeader(
            int width,
            int height
    ) {
        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        outputStream.writeBytes(new byte[]{
                (byte) 0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
        });

        byte[] imageHeader = ByteBuffer.allocate(13)
                .putInt(width)
                .putInt(height)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0)
                .array();

        writePngChunk(outputStream, "IHDR", imageHeader);

        return new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                outputStream.toByteArray()
        );
    }

    private void writePngChunk(
            ByteArrayOutputStream outputStream,
            String type,
            byte[] data
    ) {
        byte[] typeBytes = type.getBytes(
                java.nio.charset.StandardCharsets.US_ASCII
        );

        outputStream.writeBytes(
                ByteBuffer.allocate(Integer.BYTES)
                        .putInt(data.length)
                        .array()
        );
        outputStream.writeBytes(typeBytes);
        outputStream.writeBytes(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);

        outputStream.writeBytes(
                ByteBuffer.allocate(Integer.BYTES)
                        .putInt((int) crc.getValue())
                        .array()
        );
    }
}
