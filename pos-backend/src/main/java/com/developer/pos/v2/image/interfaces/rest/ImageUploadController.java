package com.developer.pos.v2.image.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import com.developer.pos.v2.image.infrastructure.persistence.entity.ImageAssetEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v2/admin/catalog/images")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        ImageAssetEntity saved = imageUploadService.upload(file);
        return ApiResponse.success(new UploadResponse(saved.getImageId(), saved.getContentType(), saved.getFileSizeBytes()));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable String imageId) {
        imageUploadService.delete(imageId);
        return ResponseEntity.noContent().build();
    }

    public record UploadResponse(String imageId, String contentType, long fileSizeBytes) {}
}
