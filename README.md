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


### Screenshots

<img width="1210" alt="Screenshot 2025-06-28 at 8 44 21 AM" src="https://github.com/user-attachments/assets/33b2401f-4d01-41a9-905c-6b905e0e6f18" />
<img width="1288" alt="Screenshot 2025-06-28 at 8 44 27 AM" src="https://github.com/user-attachments/assets/fe30685a-c713-41b5-897d-ea31c63d7347" />
<img width="1437" alt="Screenshot 2025-06-28 at 8 45 02 AM" src="https://github.com/user-attachments/assets/cb26160e-1e15-4864-9805-5c874d2ad6b4" />




