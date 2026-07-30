package com.example.imagehostingservice.image.thumbnail;

import com.example.imagehostingservice.exception.InvalidImageException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ThumbnailGenerator {

    private static final int THUMBNAIL_SIZE = 100;

    public byte[] generate(MultipartFile file) {


        try (
                InputStream inputStream = file.getInputStream();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            Thumbnails.of(inputStream)
                    .crop(Positions.CENTER)
                    .size(
                            THUMBNAIL_SIZE,
                            THUMBNAIL_SIZE
                    )
                    .outputFormat("png")
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new InvalidImageException(
                    "Could not generate image thumbnail",
                    exception
            );
        }

    }
}