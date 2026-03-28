package com.developer.pos.v2.image.infrastructure.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Local disk image storage. Replaces S3 for single-VPS deployment.
 * Images stored at {storage.base-path}/{key}.
 * Interface kept compatible so switching back to S3 is a one-file change.
 */
@Component
public class S3ImageStorage {

    private final Path basePath;

    public S3ImageStorage(
            @Value("${storage.base-path:/data/founderpos/images}") String basePath
    ) {
        this.basePath = Path.of(basePath);
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create image storage directory: " + basePath, e);
        }
    }

    public void upload(String key, byte[] data, String contentType) {
        try {
            Path filePath = basePath.resolve(key);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + key, e);
        }
    }

    public S3ObjectResponse download(String key) {
        try {
            Path filePath = basePath.resolve(key);
            if (!Files.exists(filePath)) {
                return null;
            }
            byte[] data = Files.readAllBytes(filePath);
            return new S3ObjectResponse(new ByteArrayInputStream(data), null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + key, e);
        }
    }

    public record S3ObjectResponse(InputStream inputStream, String eTag) {}
}
