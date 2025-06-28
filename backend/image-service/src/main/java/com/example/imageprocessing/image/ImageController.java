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
    private final ImageTransformationService transformationService;
    private final TransformedImageService transformedImageService;

    public ImageController(ImageService service, ImageTransformationService transformationService, 
                          TransformedImageService transformedImageService) {
        this.service = service;
        this.transformationService = transformationService;
        this.transformedImageService = transformedImageService;
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
                    .map(image -> new ImageResponse(image, service, auth.getName()))
                    .toList();
            return ResponseEntity.ok(responses);
        } else {
            List<Image> images = service.getUserImages(auth.getName());
            List<ImageResponse> responses = images.stream()
                    .map(image -> new ImageResponse(image, service, auth.getName()))
                    .toList();
            return ResponseEntity.ok(responses);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImage(@PathVariable Long id, Authentication auth) {
        return service.getImageById(id, auth.getName())
                .map(image -> ResponseEntity.ok(new ImageResponse(image, service, auth.getName())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/transform")
    public ResponseEntity<?> transformImage(
            @PathVariable Long id,
            @RequestBody TransformationRequest request,
            Authentication auth) {
        
        try {
            // Get the original image and verify user ownership
            Image originalImage = service.getImageById(id, auth.getName())
                    .orElseThrow(() -> new RuntimeException("Image not found or access denied"));

            // Convert request to transformation options
            ImageTransformationService.TransformationOptions options = convertToTransformationOptions(request.getTransformations());

            // Apply transformations
            TransformedImage result = transformationService.transformImage(originalImage, options, auth.getName());

            // Create response
            TransformationResponse response = new TransformationResponse();
            response.setOriginalImageId(result.getOriginalImageId());
            response.setTransformedImageId(result.getId());
            response.setTransformedUrl(result.getS3Url());
            response.setTransformedFilename(result.getTransformedFilename());
            response.setFileSize(result.getFileSize());
            response.setTransformations(request.getTransformations());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Transformation failed: " + e.getMessage());
        }
    }

    private ImageTransformationService.TransformationOptions convertToTransformationOptions(TransformationRequest.TransformationOptions source) {
        ImageTransformationService.TransformationOptions target = new ImageTransformationService.TransformationOptions();
        
        if (source.getResize() != null) {
            ImageTransformationService.ResizeOptions resize = new ImageTransformationService.ResizeOptions();
            resize.setWidth(source.getResize().getWidth());
            resize.setHeight(source.getResize().getHeight());
            target.setResize(resize);
        }
        
        if (source.getCrop() != null) {
            ImageTransformationService.CropOptions crop = new ImageTransformationService.CropOptions();
            crop.setWidth(source.getCrop().getWidth());
            crop.setHeight(source.getCrop().getHeight());
            crop.setX(source.getCrop().getX());
            crop.setY(source.getCrop().getY());
            target.setCrop(crop);
        }
        
        target.setRotate(source.getRotate());
        target.setFormat(source.getFormat());
        
        if (source.getFilters() != null) {
            ImageTransformationService.FilterOptions filters = new ImageTransformationService.FilterOptions();
            filters.setGrayscale(source.getFilters().getGrayscale());
            filters.setSepia(source.getFilters().getSepia());
            target.setFilters(filters);
        }
        
        return target;
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
        return ResponseEntity.ok(new ImageResponse(image, service, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id, Authentication auth) {
        try {
            service.deleteImage(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Failed to delete image: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/transformations")
    public ResponseEntity<List<TransformedImageResponse>> getImageTransformations(
            @PathVariable Long id,
            Authentication auth) {
        
        try {
            // Verify user owns the original image
            if (service.getImageById(id, auth.getName()).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            List<TransformedImage> transformations = transformedImageService.getTransformationsForImage(id, auth.getName());
            List<TransformedImageResponse> responses = transformations.stream()
                    .map(t -> new TransformedImageResponse(t, service, auth.getName()))
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/transformed-images/{id}/download-url")
    public ResponseEntity<DownloadUrlResponse> generateTransformedImageDownloadUrl(
            @PathVariable Long id,
            Authentication auth) {
        
        try {
            String presignedUrl = transformedImageService.generatePresignedDownloadUrlForTransformedImage(id, auth.getName());
            DownloadUrlResponse response = new DownloadUrlResponse();
            response.setDownloadUrl(presignedUrl);
            response.setExpiresIn(3600); // 1 hour in seconds
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/transformed-images")
    public ResponseEntity<List<TransformedImageResponse>> getAllTransformedImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        
        try {
            List<TransformedImage> transformedImages = transformedImageService.getUserTransformedImages(auth.getName());
            List<TransformedImageResponse> responses = transformedImages.stream()
                    .map(t -> new TransformedImageResponse(t, service, auth.getName()))
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/transformed-images/{id}")
    public ResponseEntity<?> deleteTransformedImage(@PathVariable Long id, Authentication auth) {
        try {
            transformedImageService.deleteTransformedImage(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Failed to delete transformed image: " + e.getMessage());
        }
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

        public ImageResponse(Image image, ImageService imageService, String username) {
            this.id = image.getId();
            this.name = image.getOriginalName();
            this.contentType = image.getContentType();
            this.fileSize = image.getFileSize();
            this.createdAt = image.getCreatedAt().toString();
            
            // Generate pre-signed URL for display
            try {
                this.url = imageService.generatePresignedDownloadUrlForImage(image.getId(), username);
            } catch (Exception e) {
                // Fallback to direct S3 URL if pre-signed URL generation fails
                this.url = image.getS3Url();
            }
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
        private TransformationRequest.TransformationOptions transformations;

        public TransformationRequest.TransformationOptions getTransformations() { return transformations; }
        public void setTransformations(TransformationRequest.TransformationOptions transformations) { this.transformations = transformations; }

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

    public static class TransformationResponse {
        private Long originalImageId;
        private Long transformedImageId;
        private String transformedUrl;
        private String transformedFilename;
        private Long fileSize;
        private TransformationRequest.TransformationOptions transformations;

        public Long getOriginalImageId() { return originalImageId; }
        public void setOriginalImageId(Long originalImageId) { this.originalImageId = originalImageId; }
        public Long getTransformedImageId() { return transformedImageId; }
        public void setTransformedImageId(Long transformedImageId) { this.transformedImageId = transformedImageId; }
        public String getTransformedUrl() { return transformedUrl; }
        public void setTransformedUrl(String transformedUrl) { this.transformedUrl = transformedUrl; }
        public String getTransformedFilename() { return transformedFilename; }
        public void setTransformedFilename(String transformedFilename) { this.transformedFilename = transformedFilename; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public TransformationRequest.TransformationOptions getTransformations() { return transformations; }
        public void setTransformations(TransformationRequest.TransformationOptions transformations) { this.transformations = transformations; }
    }

    public static class TransformedImageResponse {
        private Long id;
        private Long originalImageId;
        private String name;
        private String url;
        private String contentType;
        private Long fileSize;
        private String transformations;
        private String createdAt;

        public TransformedImageResponse(TransformedImage transformedImage, ImageService imageService, String username) {
            this.id = transformedImage.getId();
            this.originalImageId = transformedImage.getOriginalImageId();
            this.name = transformedImage.getTransformedFilename();
            this.contentType = transformedImage.getContentType();
            this.fileSize = transformedImage.getFileSize();
            this.transformations = transformedImage.getTransformations();
            this.createdAt = transformedImage.getCreatedAt().toString();
            
            // Generate pre-signed URL for secure display
            try {
                this.url = imageService.generatePresignedDownloadUrl(transformedImage.getTransformedFilename());
            } catch (Exception e) {
                // Fallback to direct S3 URL if pre-signed URL generation fails
                this.url = transformedImage.getS3Url();
            }
        }

        // Getters
        public Long getId() { return id; }
        public Long getOriginalImageId() { return originalImageId; }
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getContentType() { return contentType; }
        public Long getFileSize() { return fileSize; }
        public String getTransformations() { return transformations; }
        public String getCreatedAt() { return createdAt; }
    }
}
