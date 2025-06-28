package com.example.imageprocessing.image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformedImageRepository extends JpaRepository<TransformedImage, Long> {
    
    // Find all transformed images for a specific user
    List<TransformedImage> findByUserId(Long userId);
    
    // Find transformed images for a specific user with pagination
    Page<TransformedImage> findByUserId(Long userId, Pageable pageable);
    
    // Find all transformations of a specific original image for a user
    List<TransformedImage> findByOriginalImageIdAndUserId(Long originalImageId, Long userId);
    
    // Find a specific transformed image by ID and user (for security)
    Optional<TransformedImage> findByIdAndUserId(Long id, Long userId);
    
    // Find transformed image by filename (useful for S3 operations)
    Optional<TransformedImage> findByTransformedFilename(String filename);
    
    // Count transformations for a specific original image
    @Query("SELECT COUNT(t) FROM TransformedImage t WHERE t.originalImageId = :originalImageId AND t.userId = :userId")
    long countByOriginalImageIdAndUserId(@Param("originalImageId") Long originalImageId, @Param("userId") Long userId);
    
    // Delete all transformations for a specific original image (useful when deleting original)
    void deleteByOriginalImageIdAndUserId(Long originalImageId, Long userId);
    
    // Find recent transformations for a user
    @Query("SELECT t FROM TransformedImage t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<TransformedImage> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}