import React, { useState, useEffect } from 'react';
import axios from 'axios';
import ImageTransform from './ImageTransform';

function Dashboard({ token, onLogout }) {
  const [file, setFile] = useState(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [images, setImages] = useState([]);
  const [transformedImages, setTransformedImages] = useState([]);
  const [loadingImages, setLoadingImages] = useState(false);
  const [transformingImage, setTransformingImage] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [downloadingImage, setDownloadingImage] = useState(null);
  const [deletingImage, setDeletingImage] = useState(null);

  useEffect(() => {
    loadImages();
    loadTransformedImages();
  }, []);

  const loadImages = async () => {
    setLoadingImages(true);
    try {
      const res = await axios.get('/images', { 
        headers: { Authorization: `Bearer ${token}` } 
      });
      setImages(res.data);
    } catch (error) {
      console.log('No images endpoint available yet');
      setImages([]);
    } finally {
      setLoadingImages(false);
    }
  };

  const loadTransformedImages = async () => {
    try {
      const res = await axios.get('/images/transformed-images', { 
        headers: { Authorization: `Bearer ${token}` } 
      });
      setTransformedImages(res.data);
    } catch (error) {
      console.log('No transformed images available yet');
      setTransformedImages([]);
    }
  };

  const upload = async () => {
    if (!file) {
      setMessage('Please select a file to upload');
      return;
    }

    setLoading(true);
    setUploadProgress(0);
    
    try {
      // Step 1: Get pre-signed upload URL
      const uploadUrlResponse = await axios.post('/images/upload-url', {
        filename: file.name,
        contentType: file.type
      }, { 
        headers: { Authorization: `Bearer ${token}` } 
      });

      const { uploadUrl, filename } = uploadUrlResponse.data;
      setUploadProgress(25);

      // Step 2: Upload directly to S3 using pre-signed URL
      await axios.put(uploadUrl, file, {
        headers: {
          'Content-Type': file.type
        },
        onUploadProgress: (progressEvent) => {
          const progress = Math.round(25 + (progressEvent.loaded * 50) / progressEvent.total);
          setUploadProgress(progress);
        }
      });

      setUploadProgress(75);

      // Step 3: Save metadata to our backend
      const metadataResponse = await axios.post('/images/save-metadata', {
        filename: filename,
        originalName: file.name,
        contentType: file.type,
        fileSize: file.size
      }, { 
        headers: { Authorization: `Bearer ${token}` } 
      });

      setUploadProgress(100);
      setMessage('Upload successful!');
      setFile(null);
      loadImages();
      document.getElementById('file-input').value = '';
    } catch (error) {
      setMessage('Upload failed: ' + (error.response?.data || error.message));
    } finally {
      setLoading(false);
      setTimeout(() => setUploadProgress(0), 1000);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    onLogout();
  };

  const handleTransformImage = (image) => {
    setTransformingImage(image);
  };

  const handleTransformClose = () => {
    setTransformingImage(null);
  };

  const handleTransformed = () => {
    loadImages(); // Refresh the image list
    loadTransformedImages(); // Refresh transformed images
    setTransformingImage(null);
  };

  const handleDownloadImage = async (image) => {
    setDownloadingImage(image.id);
    try {
      const response = await axios.get(`/images/${image.id}/download-url`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      // Create a temporary anchor element to trigger download
      const link = document.createElement('a');
      link.href = response.data.downloadUrl;
      link.download = image.name || 'image';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setMessage('Download started successfully!');
    } catch (error) {
      setMessage('Download failed: ' + (error.response?.data || error.message));
    } finally {
      setDownloadingImage(null);
    }
  };

  const handleDeleteImage = async (image) => {
    if (!window.confirm(`Are you sure you want to delete "${image.name}"? This action cannot be undone.`)) {
      return;
    }

    setDeletingImage(image.id);
    try {
      await axios.delete(`/images/${image.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setMessage('Image deleted successfully!');
      loadImages(); // Refresh the image list
      loadTransformedImages(); // Refresh transformed images in case some were deleted
    } catch (error) {
      setMessage('Delete failed: ' + (error.response?.data || error.message));
    } finally {
      setDeletingImage(null);
    }
  };

  const handleDownloadTransformedImage = async (transformedImage) => {
    setDownloadingImage(transformedImage.id);
    try {
      const response = await axios.get(`/images/transformed-images/${transformedImage.id}/download-url`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      // Create a temporary anchor element to trigger download
      const link = document.createElement('a');
      link.href = response.data.downloadUrl;
      link.download = transformedImage.name || 'transformed-image';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setMessage('Download started successfully!');
    } catch (error) {
      setMessage('Download failed: ' + (error.response?.data || error.message));
    } finally {
      setDownloadingImage(null);
    }
  };

  const handleDeleteTransformedImage = async (transformedImage) => {
    if (!window.confirm(`Are you sure you want to delete this transformed image? This action cannot be undone.`)) {
      return;
    }

    setDeletingImage(transformedImage.id);
    try {
      await axios.delete(`/images/transformed-images/${transformedImage.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setMessage('Transformed image deleted successfully!');
      loadTransformedImages(); // Refresh transformed images
    } catch (error) {
      setMessage('Delete failed: ' + (error.response?.data || error.message));
    } finally {
      setDeletingImage(null);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2>Dashboard</h2>
        <button 
          onClick={handleLogout}
          style={{ 
            padding: '8px 16px', 
            backgroundColor: '#dc3545', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Logout
        </button>
      </div>
      
      <p>Welcome! You are logged in and ready to upload images.</p>
      
      {message && (
        <div style={{ 
          padding: '10px', 
          marginBottom: '15px', 
          backgroundColor: message.includes('successful') ? '#d4edda' : '#f8d7da', 
          color: message.includes('successful') ? '#155724' : '#721c24',
          borderRadius: '4px' 
        }}>
          {message}
        </div>
      )}

      <div style={{ marginBottom: '20px', padding: '20px', border: '1px solid #ddd', borderRadius: '8px' }}>
        <h3>Upload New Image</h3>
        <input 
          id="file-input"
          type="file" 
          accept="image/*"
          onChange={e => setFile(e.target.files[0])}
          style={{ marginBottom: '10px' }}
          disabled={loading}
        />
        <br />
        
        {uploadProgress > 0 && (
          <div style={{ marginBottom: '10px' }}>
            <div style={{ 
              width: '100%', 
              backgroundColor: '#f0f0f0', 
              borderRadius: '4px', 
              height: '8px',
              marginBottom: '5px'
            }}>
              <div style={{
                width: `${uploadProgress}%`,
                backgroundColor: '#28a745',
                height: '100%',
                borderRadius: '4px',
                transition: 'width 0.3s ease'
              }} />
            </div>
            <small style={{ color: '#666' }}>Upload progress: {uploadProgress}%</small>
          </div>
        )}
        
        <button 
          onClick={upload}
          disabled={loading || !file}
          style={{ 
            padding: '10px 20px', 
            backgroundColor: loading || !file ? '#6c757d' : '#28a745', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            cursor: loading || !file ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Uploading...' : 'Upload Image'}
        </button>
      </div>

      <div>
        <h3>Your Original Images</h3>
        {loadingImages ? (
          <p>Loading images...</p>
        ) : images.length > 0 ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '15px' }}>
            {images.map((image, index) => (
              <div key={index} style={{ border: '1px solid #007bff', borderRadius: '8px', padding: '10px', backgroundColor: '#f8f9ff' }}>
                <div style={{ position: 'relative', width: '100%', height: '150px', marginBottom: '10px' }}>
                  <img 
                    src={image.url} 
                    alt={image.name || 'Uploaded image'} 
                    style={{ 
                      width: '100%', 
                      height: '100%', 
                      objectFit: 'cover', 
                      borderRadius: '4px',
                      display: 'block'
                    }}
                    onError={(e) => {
                      e.target.style.display = 'none';
                      e.target.nextSibling.style.display = 'flex';
                    }}
                    onLoad={(e) => {
                      e.target.style.display = 'block';
                      e.target.nextSibling.style.display = 'none';
                    }}
                  />
                  <div 
                    style={{ 
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      width: '100%', 
                      height: '100%', 
                      backgroundColor: '#f8f9fa',
                      borderRadius: '4px',
                      display: 'none',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: '2px dashed #dee2e6',
                      color: '#6c757d'
                    }}
                  >
                    <div style={{ fontSize: '24px', marginBottom: '8px' }}>📷</div>
                    <div style={{ fontSize: '12px', textAlign: 'center' }}>
                      Image not available
                    </div>
                  </div>
                </div>
                <p style={{ margin: '0 0 10px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>
                  {image.name || 'Image'}
                </p>
                <div style={{ fontSize: '11px', color: '#999', marginBottom: '10px' }}>
                  <div>Size: {image.fileSize ? (image.fileSize / 1024).toFixed(1) + ' KB' : 'Unknown'}</div>
                  <div>Type: {image.contentType || 'Unknown'}</div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <button
                    onClick={() => handleTransformImage(image)}
                    style={{
                      width: '100%',
                      padding: '8px',
                      backgroundColor: '#007bff',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                  >
                    Transform Image
                  </button>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      onClick={() => handleDownloadImage(image)}
                      disabled={downloadingImage === image.id}
                      style={{
                        flex: 1,
                        padding: '6px',
                        backgroundColor: downloadingImage === image.id ? '#6c757d' : '#28a745',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: downloadingImage === image.id ? 'not-allowed' : 'pointer',
                        fontSize: '11px'
                      }}
                    >
                      {downloadingImage === image.id ? 'Downloading...' : 'Download'}
                    </button>
                    <button
                      onClick={() => handleDeleteImage(image)}
                      disabled={deletingImage === image.id}
                      style={{
                        flex: 1,
                        padding: '6px',
                        backgroundColor: deletingImage === image.id ? '#6c757d' : '#dc3545',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: deletingImage === image.id ? 'not-allowed' : 'pointer',
                        fontSize: '11px'
                      }}
                    >
                      {deletingImage === image.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: '#666', fontStyle: 'italic' }}>No images uploaded yet. Upload your first image above!</p>
        )}
      </div>

      {/* Transformed Images Section */}
      <div style={{ marginTop: '40px' }}>
        <h3>Your Transformed Images</h3>
        {transformedImages.length > 0 ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '15px' }}>
            {transformedImages.map((transformedImage, index) => (
              <div key={index} style={{ border: '1px solid #28a745', borderRadius: '8px', padding: '10px', backgroundColor: '#f8fff9' }}>
                <div style={{ position: 'relative', width: '100%', height: '150px', marginBottom: '10px' }}>
                  <img 
                    src={transformedImage.url} 
                    alt="Transformed image" 
                    style={{ 
                      width: '100%', 
                      height: '100%', 
                      objectFit: 'cover', 
                      borderRadius: '4px',
                      display: 'block'
                    }}
                    onError={(e) => {
                      e.target.style.display = 'none';
                      e.target.nextSibling.style.display = 'flex';
                    }}
                    onLoad={(e) => {
                      e.target.style.display = 'block';
                      e.target.nextSibling.style.display = 'none';
                    }}
                  />
                  <div 
                    style={{ 
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      width: '100%', 
                      height: '100%', 
                      backgroundColor: '#f8f9fa',
                      borderRadius: '4px',
                      display: 'none',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: '2px dashed #dee2e6',
                      color: '#6c757d'
                    }}
                  >
                    <div style={{ fontSize: '24px', marginBottom: '8px' }}>🎨</div>
                    <div style={{ fontSize: '12px', textAlign: 'center' }}>
                      Transformed image
                    </div>
                  </div>
                </div>
                <p style={{ margin: '0 0 10px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>
                  Transformed Image
                </p>
                <div style={{ fontSize: '11px', color: '#999', marginBottom: '10px' }}>
                  <div>Size: {transformedImage.fileSize ? (transformedImage.fileSize / 1024).toFixed(1) + ' KB' : 'Unknown'}</div>
                  <div>Type: {transformedImage.contentType || 'Unknown'}</div>
                  <div>Original ID: {transformedImage.originalImageId}</div>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => handleDownloadTransformedImage(transformedImage)}
                    disabled={downloadingImage === transformedImage.id}
                    style={{
                      flex: 1,
                      padding: '6px',
                      backgroundColor: downloadingImage === transformedImage.id ? '#6c757d' : '#28a745',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: downloadingImage === transformedImage.id ? 'not-allowed' : 'pointer',
                      fontSize: '11px'
                    }}
                  >
                    {downloadingImage === transformedImage.id ? 'Downloading...' : 'Download'}
                  </button>
                  <button
                    onClick={() => handleDeleteTransformedImage(transformedImage)}
                    disabled={deletingImage === transformedImage.id}
                    style={{
                      flex: 1,
                      padding: '6px',
                      backgroundColor: deletingImage === transformedImage.id ? '#6c757d' : '#dc3545',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: deletingImage === transformedImage.id ? 'not-allowed' : 'pointer',
                      fontSize: '11px'
                    }}
                  >
                    {deletingImage === transformedImage.id ? 'Deleting...' : 'Delete'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: '#666', fontStyle: 'italic' }}>No transformed images yet. Transform an image above to see them here!</p>
        )}
      </div>
      
      {transformingImage && (
        <ImageTransform
          image={transformingImage}
          token={token}
          onClose={handleTransformClose}
          onTransformed={handleTransformed}
        />
      )}
    </div>
  );
}

export default Dashboard;