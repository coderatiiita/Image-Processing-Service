package com.example.imageprocessing.image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {
    private final ImageService service;

    public ImageController(ImageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ImageResponse> upload(MultipartFile file, Authentication auth) throws Exception {
        Image image = service.upload(file, auth.getName());
        ImageResponse response = new ImageResponse(image);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ImageResponse>> getImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        
        if (limit > 0) {
            Pageable pageable = PageRequest.of(page, limit);
            Page<Image> imagePage = service.getUserImages(auth.getName(), pageable);
            List<ImageResponse> responses = imagePage.getContent().stream()
                    .map(ImageResponse::new)
                    .toList();
            return ResponseEntity.ok(responses);
        } else {
            List<Image> images = service.getUserImages(auth.getName());
            List<ImageResponse> responses = images.stream()
                    .map(ImageResponse::new)
                    .toList();
            return ResponseEntity.ok(responses);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImage(@PathVariable Long id, Authentication auth) {
        return service.getImageById(id, auth.getName())
                .map(image -> ResponseEntity.ok(new ImageResponse(image)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/transform")
    public ResponseEntity<String> transformImage(
            @PathVariable Long id,
            @RequestBody TransformationRequest request,
            Authentication auth) {
        
        // For now, return a placeholder response
        // TODO: Implement actual image transformations
        return ResponseEntity.ok("Transformation feature coming soon for image ID: " + id);
    }

    @PostMapping("/upload-url")
    public ResponseEntity<?> generateUploadUrl(
            @RequestBody UploadUrlRequest request,
            Authentication auth) {
        
        try {
            ImageService.PresignedUploadResponse response = service.generatePresignedUploadResponse(
                    request.getFilename(), 
                    request.getContentType(), 
                    auth.getName()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to generate upload URL: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<DownloadUrlResponse> generateDownloadUrl(
            @PathVariable Long id,
            Authentication auth) {
        
        try {
            String presignedUrl = service.generatePresignedDownloadUrlForImage(id, auth.getName());
            DownloadUrlResponse response = new DownloadUrlResponse();
            response.setDownloadUrl(presignedUrl);
            response.setExpiresIn(3600); // 1 hour in seconds
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/save-metadata")
    public ResponseEntity<ImageResponse> saveImageMetadata(
            @RequestBody SaveMetadataRequest request,
            Authentication auth) {
        
        Image image = service.saveImageMetadata(
                request.getFilename(),
                request.getOriginalName(),
                request.getContentType(),
                request.getFileSize(),
                auth.getName()
        );
        return ResponseEntity.ok(new ImageResponse(image));
    }

    @GetMapping("/config")
    public ResponseEntity<String> getS3Config() {
        return ResponseEntity.ok("S3 Configuration - Bucket: " + service.getBucket() + ", Region: " + service.getRegion());
    }

    // DTO classes
    public static class ImageResponse {
        private Long id;
        private String name;
        private String url;
        private String contentType;
        private Long fileSize;
        private String createdAt;

        public ImageResponse(Image image) {
            this.id = image.getId();
            this.name = image.getOriginalName();
            this.url = image.getS3Url();
            this.contentType = image.getContentType();
            this.fileSize = image.getFileSize();
            this.createdAt = image.getCreatedAt().toString();
        }

        // getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getContentType() { return contentType; }
        public Long getFileSize() { return fileSize; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class TransformationRequest {
        private TransformationOptions transformations;

        public TransformationOptions getTransformations() { return transformations; }
        public void setTransformations(TransformationOptions transformations) { this.transformations = transformations; }

        public static class TransformationOptions {
            private ResizeOptions resize;
            private CropOptions crop;
            private Integer rotate;
            private String format;
            private FilterOptions filters;

            // getters and setters
            public ResizeOptions getResize() { return resize; }
            public void setResize(ResizeOptions resize) { this.resize = resize; }
            public CropOptions getCrop() { return crop; }
            public void setCrop(CropOptions crop) { this.crop = crop; }
            public Integer getRotate() { return rotate; }
            public void setRotate(Integer rotate) { this.rotate = rotate; }
            public String getFormat() { return format; }
            public void setFormat(String format) { this.format = format; }
            public FilterOptions getFilters() { return filters; }
            public void setFilters(FilterOptions filters) { this.filters = filters; }
        }

        public static class ResizeOptions {
            private Integer width;
            private Integer height;

            public Integer getWidth() { return width; }
            public void setWidth(Integer width) { this.width = width; }
            public Integer getHeight() { return height; }
            public void setHeight(Integer height) { this.height = height; }
        }

        public static class CropOptions {
            private Integer width;
            private Integer height;
            private Integer x;
            private Integer y;

            public Integer getWidth() { return width; }
            public void setWidth(Integer width) { this.width = width; }
            public Integer getHeight() { return height; }
            public void setHeight(Integer height) { this.height = height; }
            public Integer getX() { return x; }
            public void setX(Integer x) { this.x = x; }
            public Integer getY() { return y; }
            public void setY(Integer y) { this.y = y; }
        }

        public static class FilterOptions {
            private Boolean grayscale;
            private Boolean sepia;

            public Boolean getGrayscale() { return grayscale; }
            public void setGrayscale(Boolean grayscale) { this.grayscale = grayscale; }
            public Boolean getSepia() { return sepia; }
            public void setSepia(Boolean sepia) { this.sepia = sepia; }
        }
    }

    public static class UploadUrlRequest {
        private String filename;
        private String contentType;

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    public static class DownloadUrlResponse {
        private String downloadUrl;
        private int expiresIn;

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
        public int getExpiresIn() { return expiresIn; }
        public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
    }

    public static class SaveMetadataRequest {
        private String filename;
        private String originalName;
        private String contentType;
        private Long fileSize;

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    }
}
