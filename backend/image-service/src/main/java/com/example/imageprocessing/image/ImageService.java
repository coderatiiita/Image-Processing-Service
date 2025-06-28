package com.example.imageprocessing.image;

import com.example.imageprocessing.jwt.JwtUtil;
import com.example.imageprocessing.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    @Value("${aws.s3.bucket:project-image-processing-service-bucket}")
    private String bucket;
    
    @Value("${aws.region:ap-south-1}")
    private String region;

    public ImageService(ImageRepository imageRepository, UserRepository userRepository, JwtUtil jwtUtil, 
                       S3Client s3Client, S3Presigner s3Presigner) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public Image upload(MultipartFile file, String username) throws IOException {
        // Get user ID
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String filename = UUID.randomUUID().toString() + "_" + originalName;
        
        // Upload to S3
        java.nio.file.Path temp = Files.createTempFile("upload", originalName);
        file.transferTo(temp);
        s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(filename).build(), temp);
        Files.deleteIfExists(temp);
        
        String s3Url = generateS3Url(filename);
        
        // Save to database
        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalName(originalName);
        image.setS3Url(s3Url);
        image.setContentType(file.getContentType());
        image.setFileSize(file.getSize());
        image.setUserId(userId);
        
        return imageRepository.save(image);
    }

    public List<Image> getUserImages(String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        return imageRepository.findByUserId(userId);
    }

    public Page<Image> getUserImages(String username, Pageable pageable) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        return imageRepository.findByUserId(userId, pageable);
    }

    public Optional<Image> getImageById(Long imageId, String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        return imageRepository.findByIdAndUserId(imageId, userId);
    }

    public String generatePresignedUploadUrl(String filename, String contentType, String username) {
        // Get user ID to ensure authorization (don't store in metadata for pre-signed URLs)
        userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate unique filename
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uniqueFilename)
                .contentType(contentType)
                // Remove metadata from pre-signed URLs to avoid signature issues
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15)) // URL expires in 15 minutes
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String generatePresignedDownloadUrl(String filename) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(filename)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1)) // URL expires in 1 hour
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public String generatePresignedDownloadUrlForImage(Long imageId, String username) {
        Optional<Image> imageOpt = getImageById(imageId, username);
        if (imageOpt.isEmpty()) {
            throw new RuntimeException("Image not found or access denied");
        }
        
        Image image = imageOpt.get();
        return generatePresignedDownloadUrl(image.getFilename());
    }

    public PresignedUploadResponse generatePresignedUploadResponse(String filename, String contentType, String username) {
        // Generate unique filename first so we can return it
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;
        
        // Generate presigned URL with the unique filename
        String presignedUrl = generatePresignedUploadUrlWithFilename(uniqueFilename, contentType, username);
        
        PresignedUploadResponse response = new PresignedUploadResponse();
        response.setUploadUrl(presignedUrl);
        response.setFilename(uniqueFilename);
        response.setExpiresIn(900); // 15 minutes in seconds
        
        return response;
    }
    
    private String generatePresignedUploadUrlWithFilename(String uniqueFilename, String contentType, String username) {
        // Get user ID to ensure authorization
        userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        logger.info("Generating pre-signed upload URL for file: {} with content-type: {} in bucket: {} region: {}", 
                   uniqueFilename, contentType, bucket, region);
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uniqueFilename)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        logger.debug("Generated pre-signed URL: {}", presignedUrl);
        
        return presignedUrl;
    }


    public Image saveImageMetadata(String filename, String originalName, String contentType, Long fileSize, String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        String s3Url = generateS3Url(filename);
        
        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalName(originalName);
        image.setS3Url(s3Url);
        image.setContentType(contentType);
        image.setFileSize(fileSize);
        image.setUserId(userId);
        
        return imageRepository.save(image);
    }
    
    // Getter methods for configuration
    public String getBucket() {
        return bucket;
    }
    
    public String getRegion() {
        return region;
    }

    private String generateS3Url(String filename) {
        // For regions other than us-east-1, use regional endpoint format
        if ("us-east-1".equals(region)) {
            return String.format("https://%s.s3.amazonaws.com/%s", bucket, filename);
        } else {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, filename);
        }
    }

    public void deleteImage(Long imageId, String username) {
        // Get user ID and verify ownership
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        // Find the image and verify ownership
        Image image = imageRepository.findByIdAndUserId(imageId, userId)
                .orElseThrow(() -> new RuntimeException("Image not found or access denied"));
        
        try {
            // Delete from S3
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = 
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(image.getFilename())
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            logger.info("Deleted image from S3: {}", image.getFilename());
            
            // Delete from database
            imageRepository.delete(image);
            logger.info("Deleted image from database: {} (ID: {})", image.getOriginalName(), imageId);
            
        } catch (Exception e) {
            logger.error("Failed to delete image: {} (ID: {})", image.getFilename(), imageId, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage());
        }
    }

    // DTO class for presigned upload response
    public static class PresignedUploadResponse {
        private String uploadUrl;
        private String filename;
        private int expiresIn;

        public String getUploadUrl() { return uploadUrl; }
        public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public int getExpiresIn() { return expiresIn; }
        public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
    }
}
