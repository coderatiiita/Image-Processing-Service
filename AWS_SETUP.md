# AWS S3 Setup for Image Processing Service

## Prerequisites

1. **AWS Account**: Create an AWS account if you don't have one
2. **S3 Bucket**: Create an S3 bucket for storing images
3. **IAM User**: Create an IAM user with S3 permissions

## Step 1: Create S3 Bucket

1. Go to AWS S3 Console
2. Click "Create bucket"
3. Choose the bucket name: `project-image-processing-service-bucket`
4. Select the Asia Pacific (Mumbai) region: `ap-south-1`
5. Keep default settings and create the bucket

## Step 2: Create IAM User

1. Go to AWS IAM Console
2. Click "Users" → "Add user"
3. Enter username (e.g., `image-service-user`)
4. Select "Programmatic access"
5. Attach policy: `AmazonS3FullAccess` (or create custom policy)
6. Complete user creation and **save the Access Key ID and Secret Access Key**

### Custom IAM Policy (Recommended)

Instead of full S3 access, create a custom policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:GetObjectAcl",
                "s3:PutObjectAcl"
            ],
            "Resource": "arn:aws:s3:::project-image-processing-service-bucket/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": "arn:aws:s3:::project-image-processing-service-bucket"
        }
    ]
}
```

## Step 3: Configure Credentials

### Option A: Environment Variables (Recommended)

Set these environment variables:

**Linux/Mac:**
```bash
export AWS_ACCESS_KEY_ID=your_access_key_here
export AWS_SECRET_ACCESS_KEY=your_secret_key_here
export AWS_REGION=ap-south-1
export AWS_S3_BUCKET=project-image-processing-service-bucket
```

**Windows:**
```cmd
set AWS_ACCESS_KEY_ID=your_access_key_here
set AWS_SECRET_ACCESS_KEY=your_secret_key_here
set AWS_REGION=ap-south-1
set AWS_S3_BUCKET=project-image-processing-service-bucket
```

### Option B: AWS CLI Configuration

1. Install AWS CLI: https://aws.amazon.com/cli/
2. Run: `aws configure`
3. Enter your credentials when prompted

### Option C: Application Properties (Not for Production)

Edit `backend/image-service/src/main/resources/application.properties`:

```properties
aws.accessKeyId=your_access_key_here
aws.secretKey=your_secret_key_here
aws.region=ap-south-1
aws.s3.bucket=project-image-processing-service-bucket
```

**⚠️ Never commit credentials to version control!**

## Step 4: Update Bucket CORS

To allow browser uploads, configure CORS for your S3 bucket:

1. Go to your S3 bucket
2. Click "Permissions" → "Cross-origin resource sharing (CORS)"
3. Add this configuration:

```json
[
    {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
        "AllowedOrigins": ["http://localhost:3000", "https://yourdomain.com"],
        "ExposeHeaders": ["ETag"]
    }
]
```

## Step 5: Test the Setup

1. Start your Spring Boot backend: `mvn spring-boot:run`
2. Start your React frontend: `npm start`
3. Try uploading an image

## Troubleshooting

### Common Errors:

1. **403 Forbidden**: Check IAM permissions and bucket policy
2. **Credentials not found**: Verify environment variables or AWS CLI config
3. **CORS errors**: Update bucket CORS configuration
4. **Bucket not found**: Check bucket name and region
5. **SignatureDoesNotMatch**: This was fixed by removing metadata from pre-signed URLs

### Signature Issues (Fixed):
If you encounter `SignatureDoesNotMatch` errors:
- ✅ **Fixed**: Removed metadata from pre-signed URL generation
- ✅ **Fixed**: Simplified frontend upload to only include Content-Type header
- ✅ **Fixed**: Proper filename handling in pre-signed URL workflow

### Verify Credentials:

```bash
aws s3 ls s3://project-image-processing-service-bucket
```

This should list your bucket contents if credentials are configured correctly.

### Verify Backend Configuration:

1. **Check S3 Configuration**: Visit `http://localhost:8080/images/config` to verify bucket and region settings
2. **Expected Response**: `S3 Configuration - Bucket: project-image-processing-service-bucket, Region: ap-south-1`

## Region-Specific Notes for ap-south-1 (Asia Pacific - Mumbai)

### Benefits of using ap-south-1:
- **Lower Latency**: Reduced latency for users in India and South Asia
- **Data Residency**: Keeps data within India for compliance requirements
- **Cost Optimization**: Lower data transfer costs within the same region
- **Better Performance**: Improved upload/download speeds for local users

### Important Considerations:
- S3 URLs in ap-south-1 use the format: `https://project-image-processing-service-bucket.s3.ap-south-1.amazonaws.com/`
- Ensure all AWS services (EC2, RDS, etc.) are in the same region for optimal performance
- CloudFront can still be used globally with ap-south-1 as the origin

### AWS CLI Configuration for ap-south-1:
When running `aws configure`, use:
- **Default region name**: `ap-south-1`
- **Default output format**: `json`

## Production Considerations

1. Use IAM roles instead of access keys when deploying to AWS
2. Enable S3 bucket versioning and lifecycle policies
3. Set up CloudFront for better performance globally
4. Implement proper error handling and logging
5. Use environment-specific configurations
6. Consider using AWS VPC endpoints for enhanced security
7. Enable S3 transfer acceleration for global users