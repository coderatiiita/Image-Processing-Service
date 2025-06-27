package com.example.imageprocessing.image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUserId(Long userId);
    Page<Image> findByUserId(Long userId, Pageable pageable);
    Optional<Image> findByIdAndUserId(Long id, Long userId);
}