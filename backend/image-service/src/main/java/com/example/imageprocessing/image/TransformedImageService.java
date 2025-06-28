package com.example.imageprocessing.image;

import com.example.imageprocessing.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class TransformedImageService {
    private static final Logger logger = LoggerFactory.getLogger(TransformedImageService.class);

    private final TransformedImageRepository transformedImageRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:project-image-processing-service-bucket}")
    private String bucket;

    public TransformedImageService(TransformedImageRepository transformedImageRepository,
                                 UserRepository userRepository,
                                 S3Client s3Client,
                                 S3Presigner s3Presigner) {
        this.transformedImageRepository = transformedImageRepository;
        this.userRepository = userRepository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public List<TransformedImage> getTransformationsForImage(Long originalImageId, String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        return transformedImageRepository.findByOriginalImageIdAndUserId(originalImageId, userId);
    }

    public List<TransformedImage> getUserTransformedImages(String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        return transformedImageRepository.findByUserId(userId);
    }

    public Optional<TransformedImage> getTransformedImageById(Long transformedImageId, String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        return transformedImageRepository.findByIdAndUserId(transformedImageId, userId);
    }

    public String generatePresignedDownloadUrlForTransformedImage(Long transformedImageId, String username) {
        Optional<TransformedImage> transformedImageOpt = getTransformedImageById(transformedImageId, username);
        if (transformedImageOpt.isEmpty()) {
            throw new RuntimeException("Transformed image not found or access denied");
        }
        
        TransformedImage transformedImage = transformedImageOpt.get();
        return generatePresignedDownloadUrl(transformedImage.getTransformedFilename());
    }

    private String generatePresignedDownloadUrl(String filename) {
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

    public void deleteTransformedImage(Long transformedImageId, String username) {
        // Get user ID and verify ownership
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        // Find the transformed image and verify ownership
        TransformedImage transformedImage = transformedImageRepository.findByIdAndUserId(transformedImageId, userId)
                .orElseThrow(() -> new RuntimeException("Transformed image not found or access denied"));
        
        try {
            // Delete from S3
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = 
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(transformedImage.getTransformedFilename())
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            logger.info("Deleted transformed image from S3: {}", transformedImage.getTransformedFilename());
            
            // Delete from database
            transformedImageRepository.delete(transformedImage);
            logger.info("Deleted transformed image from database: {} (ID: {})", 
                       transformedImage.getTransformedFilename(), transformedImageId);
            
        } catch (Exception e) {
            logger.error("Failed to delete transformed image: {} (ID: {})", 
                        transformedImage.getTransformedFilename(), transformedImageId, e);
            throw new RuntimeException("Failed to delete transformed image: " + e.getMessage());
        }
    }

    public void deleteAllTransformationsForImage(Long originalImageId, String username) {
        Long userId = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        List<TransformedImage> transformations = transformedImageRepository.findByOriginalImageIdAndUserId(originalImageId, userId);
        
        for (TransformedImage transformation : transformations) {
            try {
                // Delete from S3
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = 
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(transformation.getTransformedFilename())
                        .build();
                
                s3Client.deleteObject(deleteRequest);
                logger.info("Deleted transformed image from S3: {}", transformation.getTransformedFilename());
                
            } catch (Exception e) {
                logger.warn("Failed to delete transformed image from S3: {}", transformation.getTransformedFilename(), e);
            }
        }
        
        // Delete all from database
        transformedImageRepository.deleteByOriginalImageIdAndUserId(originalImageId, userId);
        logger.info("Deleted all transformations for original image ID: {}", originalImageId);
    }
}