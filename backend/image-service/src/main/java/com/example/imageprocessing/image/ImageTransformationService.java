package com.example.imageprocessing.image;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImageTransformationService {
    private static final Logger logger = LoggerFactory.getLogger(ImageTransformationService.class);

    private final S3Client s3Client;
    private final ImageRepository imageRepository;
    private final TransformedImageRepository transformedImageRepository;
    private final com.example.imageprocessing.user.UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket:project-image-processing-service-bucket}")
    private String bucket;

    @Value("${aws.region:ap-south-1}")
    private String region;

    public ImageTransformationService(S3Client s3Client, ImageRepository imageRepository, 
                                     TransformedImageRepository transformedImageRepository,
                                     com.example.imageprocessing.user.UserRepository userRepository,
                                     ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.imageRepository = imageRepository;
        this.transformedImageRepository = transformedImageRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public TransformedImage transformImage(Image originalImage, TransformationOptions options, String username) throws IOException {
        logger.info("Starting transformation for image: {} with options: {}", originalImage.getFilename(), options);

        // Download original image from S3
        InputStream originalImageStream = downloadImageFromS3(originalImage.getFilename());

        // Apply transformations
        ByteArrayOutputStream transformedImageStream = new ByteArrayOutputStream();
        applyTransformations(originalImageStream, transformedImageStream, options);

        // Generate new filename for transformed image
        String transformedFilename = generateTransformedFilename(originalImage.getOriginalName(), options);

        // Upload transformed image to S3
        String outputContentType = getOutputContentType(options.getFormat(), originalImage.getContentType());
        String transformedS3Url = uploadTransformedImageToS3(transformedFilename, transformedImageStream.toByteArray(), outputContentType);

        // Get user ID
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // Convert transformation options to JSON string
        String transformationsJson = transformationOptionsToJson(options);

        // Save transformed image to database
        TransformedImage transformedImage = new TransformedImage(
            originalImage.getId(),
            userId,
            transformedFilename,
            transformedS3Url,
            outputContentType,
            (long) transformedImageStream.size(),
            transformationsJson
        );

        TransformedImage savedImage = transformedImageRepository.save(transformedImage);

        logger.info("Transformation completed for image: {} -> {}", originalImage.getFilename(), transformedFilename);
        
        return savedImage;
    }

    private InputStream downloadImageFromS3(String filename) throws IOException {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
            
            // Read the entire stream into memory
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = s3Object.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            s3Object.close();
            
            return new ByteArrayInputStream(buffer.toByteArray());
        } catch (Exception e) {
            throw new IOException("Failed to download image from S3: " + filename, e);
        }
    }

    private void applyTransformations(InputStream inputStream, ByteArrayOutputStream outputStream, 
                                    TransformationOptions options) throws IOException {
        try {
            // First, apply basic transformations using Thumbnailator
            ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
            var thumbnailBuilder = Thumbnails.of(inputStream);
            
            // If no resize is specified, keep original size
            boolean hasResize = false;

            // Apply resize transformation
            if (options.getResize() != null) {
                ResizeOptions resize = options.getResize();
                if (resize.getWidth() != null && resize.getHeight() != null) {
                    thumbnailBuilder = thumbnailBuilder.size(resize.getWidth(), resize.getHeight());
                    hasResize = true;
                } else if (resize.getWidth() != null) {
                    thumbnailBuilder = thumbnailBuilder.width(resize.getWidth());
                    hasResize = true;
                } else if (resize.getHeight() != null) {
                    thumbnailBuilder = thumbnailBuilder.height(resize.getHeight());
                    hasResize = true;
                }
            }

            // Apply crop transformation
            if (options.getCrop() != null) {
                CropOptions crop = options.getCrop();
                thumbnailBuilder = thumbnailBuilder.sourceRegion(
                    crop.getX(), crop.getY(), crop.getWidth(), crop.getHeight()
                );
            }

            // Apply rotation
            if (options.getRotate() != null && options.getRotate() != 0) {
                thumbnailBuilder = thumbnailBuilder.rotate(options.getRotate());
            }
            
            // If no size transformation was applied, scale to keep original size
            if (!hasResize && options.getCrop() == null) {
                thumbnailBuilder = thumbnailBuilder.scale(1.0);
            }

            // Set output format
            String outputFormat = getOutputFormat(options.getFormat());
            
            // Apply basic transformations to temp stream
            thumbnailBuilder.outputFormat(outputFormat).toOutputStream(tempStream);

            // Apply filters if needed using Java 2D
            if (options.getFilters() != null && (
                Boolean.TRUE.equals(options.getFilters().getGrayscale()) || 
                Boolean.TRUE.equals(options.getFilters().getSepia()))) {
                
                // Read the transformed image
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(tempStream.toByteArray()));
                
                // Apply filters
                if (Boolean.TRUE.equals(options.getFilters().getGrayscale())) {
                    image = applyGrayscaleFilter(image);
                }
                if (Boolean.TRUE.equals(options.getFilters().getSepia())) {
                    image = applySepiaFilter(image);
                }
                
                // Write the filtered image to output stream
                ImageIO.write(image, outputFormat, outputStream);
            } else {
                // No filters, just copy the temp stream to output
                outputStream.write(tempStream.toByteArray());
            }

        } catch (Exception e) {
            logger.error("Failed to apply transformations: {}", e.getMessage(), e);
            throw new IOException("Failed to apply transformations: " + e.getMessage(), e);
        }
    }

    private BufferedImage applyGrayscaleFilter(BufferedImage original) {
        ColorConvertOp colorConvert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage grayscaleImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        colorConvert.filter(original, grayscaleImage);
        return grayscaleImage;
    }

    private BufferedImage applySepiaFilter(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        BufferedImage sepiaImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                Color color = new Color(rgb);
                
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();
                
                // Apply sepia formula
                int sepiaRed = Math.min(255, (int) (0.393 * red + 0.769 * green + 0.189 * blue));
                int sepiaGreen = Math.min(255, (int) (0.349 * red + 0.686 * green + 0.168 * blue));
                int sepiaBlue = Math.min(255, (int) (0.272 * red + 0.534 * green + 0.131 * blue));
                
                Color sepiaColor = new Color(sepiaRed, sepiaGreen, sepiaBlue);
                sepiaImage.setRGB(x, y, sepiaColor.getRGB());
            }
        }
        
        return sepiaImage;
    }

    private String uploadTransformedImageToS3(String filename, byte[] imageData, String contentType) throws IOException {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .contentType(contentType)
                    .contentLength((long) imageData.length)
                    .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(imageData));

            // Generate S3 URL
            if ("us-east-1".equals(region)) {
                return String.format("https://%s.s3.amazonaws.com/%s", bucket, filename);
            } else {
                return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, filename);
            }
        } catch (Exception e) {
            throw new IOException("Failed to upload transformed image to S3: " + filename, e);
        }
    }

    private String generateTransformedFilename(String originalName, TransformationOptions options) {
        String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
        String extension = originalName.substring(originalName.lastIndexOf('.'));
        
        // Generate transformation suffix
        StringBuilder suffix = new StringBuilder("_transformed");
        
        if (options.getResize() != null) {
            ResizeOptions resize = options.getResize();
            suffix.append("_resize");
            if (resize.getWidth() != null) suffix.append("w").append(resize.getWidth());
            if (resize.getHeight() != null) suffix.append("h").append(resize.getHeight());
        }
        
        if (options.getCrop() != null) {
            suffix.append("_crop");
        }
        
        if (options.getRotate() != null && options.getRotate() != 0) {
            suffix.append("_rot").append(options.getRotate());
        }
        
        if (options.getFilters() != null) {
            if (Boolean.TRUE.equals(options.getFilters().getGrayscale())) {
                suffix.append("_gray");
            }
            if (Boolean.TRUE.equals(options.getFilters().getSepia())) {
                suffix.append("_sepia");
            }
        }

        // Change extension if format conversion is requested
        if (options.getFormat() != null && !options.getFormat().isEmpty()) {
            extension = "." + options.getFormat().toLowerCase();
        }

        return UUID.randomUUID().toString() + "_" + baseName + suffix.toString() + extension;
    }

    private String getOutputFormat(String requestedFormat) {
        if (requestedFormat == null || requestedFormat.isEmpty()) {
            return "jpg"; // Default format
        }
        
        switch (requestedFormat.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return "jpg";
            case "png":
                return "png";
            case "webp":
                return "webp";
            default:
                return "jpg";
        }
    }

    private String getOutputContentType(String requestedFormat, String originalContentType) {
        if (requestedFormat == null || requestedFormat.isEmpty()) {
            return originalContentType;
        }
        
        switch (requestedFormat.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            default:
                return originalContentType;
        }
    }

    private String transformationOptionsToJson(TransformationOptions options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            logger.warn("Failed to serialize transformation options to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    // DTO classes for transformation
    public static class TransformationResult {
        private Long originalImageId;
        private String transformedFilename;
        private String transformedUrl;
        private TransformationOptions transformations;
        private Long fileSize;

        // Getters and setters
        public Long getOriginalImageId() { return originalImageId; }
        public void setOriginalImageId(Long originalImageId) { this.originalImageId = originalImageId; }
        public String getTransformedFilename() { return transformedFilename; }
        public void setTransformedFilename(String transformedFilename) { this.transformedFilename = transformedFilename; }
        public String getTransformedUrl() { return transformedUrl; }
        public void setTransformedUrl(String transformedUrl) { this.transformedUrl = transformedUrl; }
        public TransformationOptions getTransformations() { return transformations; }
        public void setTransformations(TransformationOptions transformations) { this.transformations = transformations; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    }

    // Import transformation option classes from ImageController
    public static class TransformationOptions {
        private ResizeOptions resize;
        private CropOptions crop;
        private Integer rotate;
        private String format;
        private FilterOptions filters;

        // Getters and setters
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