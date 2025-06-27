package com.example.imageprocessing.image;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

@Service
public class ImageService {
    private final S3Client s3 = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    private final String bucket = "my-bucket"; // replace with real bucket

    public String upload(MultipartFile file) throws IOException {
        java.nio.file.Path temp = Files.createTempFile("upload", file.getOriginalFilename());
        file.transferTo(temp);
        String key = file.getOriginalFilename();
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), temp);
        Files.deleteIfExists(temp);
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
    }
}
