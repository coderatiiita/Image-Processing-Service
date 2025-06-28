package com.example.imageprocessing.image;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transformed_images")
public class TransformedImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_image_id", nullable = false)
    private Long originalImageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transformed_filename", nullable = false)
    private String transformedFilename;

    @Column(name = "s3_url", nullable = false)
    private String s3Url;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "transformations", columnDefinition = "TEXT")
    private String transformations; // JSON string of applied transformations

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Foreign key relationship to original image
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_image_id", insertable = false, updatable = false)
    private Image originalImage;

    // Constructors
    public TransformedImage() {
        this.createdAt = LocalDateTime.now();
    }

    public TransformedImage(Long originalImageId, Long userId, String transformedFilename, 
                           String s3Url, String contentType, Long fileSize, String transformations) {
        this();
        this.originalImageId = originalImageId;
        this.userId = userId;
        this.transformedFilename = transformedFilename;
        this.s3Url = s3Url;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.transformations = transformations;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOriginalImageId() { return originalImageId; }
    public void setOriginalImageId(Long originalImageId) { this.originalImageId = originalImageId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTransformedFilename() { return transformedFilename; }
    public void setTransformedFilename(String transformedFilename) { this.transformedFilename = transformedFilename; }

    public String getS3Url() { return s3Url; }
    public void setS3Url(String s3Url) { this.s3Url = s3Url; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getTransformations() { return transformations; }
    public void setTransformations(String transformations) { this.transformations = transformations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Image getOriginalImage() { return originalImage; }
    public void setOriginalImage(Image originalImage) { this.originalImage = originalImage; }
}