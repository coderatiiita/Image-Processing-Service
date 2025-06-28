# Image-Processing-Service

This project involves creating a backend system for an image processing service similar to Cloudinary. The service will allow users to upload images, perform various transformations, and retrieve images in different formats. The system will feature user authentication, image upload, transformation operations, and efficient retrieval mechanisms.

## Local Development

### Backend
1. Navigate to `backend/image-service` and build using Maven:
   ```bash
   mvn package
   ```
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The backend uses MySQL for persistence (configure credentials in `application.properties`) and AWS S3 for storing uploaded images.

### Frontend
1. Navigate to `frontend` and install dependencies:
   ```bash
   npm install
   ```
2. Start the frontend (uses a minimal development server):
   ```bash
   npm start
   ```
