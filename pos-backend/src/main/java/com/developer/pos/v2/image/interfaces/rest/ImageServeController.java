package com.developer.pos.v2.image.interfaces.rest;

import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import com.developer.pos.v2.image.infrastructure.s3.S3ImageStorage;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v2/images")
public class ImageServeController {

    private final ImageUploadService imageUploadService;
    private final S3ImageStorage s3Storage;

    public ImageServeController(ImageUploadService imageUploadService, S3ImageStorage s3Storage) {
        this.imageUploadService = imageUploadService;
        this.s3Storage = s3Storage;
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> serve(@PathVariable String imageId) throws IOException {
        ImageAssetEntity image = imageUploadService.getActiveImage(imageId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        S3ImageStorage.S3ObjectResponse s3Response = s3Storage.download(image.getS3Key());
        byte[] data = s3Response.inputStream().readAllBytes();
        String ext = image.getContentType().split("/")[1];
        if ("jpeg".equals(ext)) ext = "jpg";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(image.getContentType()));
        headers.set("Content-Disposition", "inline; filename=\"" + imageId + "." + ext + "\"");
        headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'none'");
        if (s3Response.eTag() != null) {
            headers.setETag(s3Response.eTag());
        }
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
