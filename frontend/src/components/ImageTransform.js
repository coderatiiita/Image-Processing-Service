import React, { useState } from 'react';
import axios from 'axios';

function ImageTransform({ image, token, onClose, onTransformed }) {
  const [transformations, setTransformations] = useState({
    resize: { width: '', height: '', maintainAspectRatio: true },
    crop: { width: '', height: '', x: '', y: '' },
    rotate: 0,
    format: '',
    filters: { grayscale: false, sepia: false }
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const handleTransformationChange = (category, field, value) => {
    setTransformations(prev => ({
      ...prev,
      [category]: {
        ...prev[category],
        [field]: value
      }
    }));
  };

  const handleFilterChange = (filter) => {
    setTransformations(prev => ({
      ...prev,
      filters: {
        ...prev.filters,
        [filter]: !prev.filters[filter]
      }
    }));
  };

  const applyTransformations = async () => {
    setLoading(true);
    try {
      // Clean up transformations - remove empty values
      const cleanTransformations = {};
      
      // Add resize if values provided
      if (transformations.resize.width || transformations.resize.height) {
        cleanTransformations.resize = {
          width: parseInt(transformations.resize.width) || undefined,
          height: parseInt(transformations.resize.height) || undefined
        };
      }

      // Add crop if values provided
      if (transformations.crop.width && transformations.crop.height) {
        cleanTransformations.crop = {
          width: parseInt(transformations.crop.width),
          height: parseInt(transformations.crop.height),
          x: parseInt(transformations.crop.x) || 0,
          y: parseInt(transformations.crop.y) || 0
        };
      }

      // Add rotation if not 0
      if (transformations.rotate !== 0) {
        cleanTransformations.rotate = transformations.rotate;
      }

      // Add format if selected
      if (transformations.format) {
        cleanTransformations.format = transformations.format;
      }

      // Add filters if any are enabled
      const enabledFilters = Object.entries(transformations.filters)
        .filter(([_, enabled]) => enabled)
        .reduce((acc, [filter, _]) => ({ ...acc, [filter]: true }), {});
      
      if (Object.keys(enabledFilters).length > 0) {
        cleanTransformations.filters = enabledFilters;
      }

      const response = await axios.post(
        `/images/${image.id}/transform`,
        { transformations: cleanTransformations },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setMessage('Transformation applied successfully!');
      if (onTransformed) onTransformed();
    } catch (error) {
      setMessage('Transformation failed: ' + (error.response?.data || error.message));
    } finally {
      setLoading(false);
    }
  };

  const resetTransformations = () => {
    setTransformations({
      resize: { width: '', height: '', maintainAspectRatio: true },
      crop: { width: '', height: '', x: '', y: '' },
      rotate: 0,
      format: '',
      filters: { grayscale: false, sepia: false }
    });
    setMessage('');
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      zIndex: 1000
    }}>
      <div style={{
        backgroundColor: 'white',
        padding: '20px',
        borderRadius: '8px',
        maxWidth: '600px',
        maxHeight: '80vh',
        overflowY: 'auto',
        width: '90%'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h3>Transform Image: {image.name}</h3>
          <button 
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '20px',
              cursor: 'pointer',
              color: '#666'
            }}
          >
            ×
          </button>
        </div>

        {message && (
          <div style={{
            padding: '10px',
            marginBottom: '15px',
            backgroundColor: message.includes('success') ? '#d4edda' : '#f8d7da',
            color: message.includes('success') ? '#155724' : '#721c24',
            borderRadius: '4px'
          }}>
            {message}
          </div>
        )}

        <div style={{ display: 'grid', gap: '20px' }}>
          {/* Image Preview */}
          <div>
            <h4>Current Image</h4>
            <img 
              src={image.url} 
              alt={image.name}
              style={{ 
                maxWidth: '100%', 
                height: '200px', 
                objectFit: 'contain',
                border: '1px solid #ddd',
                borderRadius: '4px'
              }}
            />
          </div>

          {/* Resize Controls */}
          <div>
            <h4>Resize</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <input
                type="number"
                placeholder="Width (px)"
                value={transformations.resize.width}
                onChange={(e) => handleTransformationChange('resize', 'width', e.target.value)}
                style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
              />
              <input
                type="number"
                placeholder="Height (px)"
                value={transformations.resize.height}
                onChange={(e) => handleTransformationChange('resize', 'height', e.target.value)}
                style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
          </div>

          {/* Crop Controls */}
          <div>
            <h4>Crop</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '10px' }}>
              <input
                type="number"
                placeholder="Width (px)"
                value={transformations.crop.width}
                onChange={(e) => handleTransformationChange('crop', 'width', e.target.value)}
                style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
              />
              <input
                type="number"
                placeholder="Height (px)"
                value={transformations.crop.height}
                onChange={(e) => handleTransformationChange('crop', 'height', e.target.value)}
                style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <input
                type="number"
                placeholder="X Position"
                value={transformations.crop.x}
                onChange={(e) => handleTransformationChange('crop', 'x', e.target.value)}
                style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
              />
              <input
                type="number"
                placeholder="Y Position"
                value={transformations.crop.y}
                onChange={(e) => handleTransformationChange('crop', 'y', e.target.value)}
                style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
              />
            </div>
          </div>

          {/* Rotation Controls */}
          <div>
            <h4>Rotation</h4>
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
              {[0, 90, 180, 270].map(angle => (
                <button
                  key={angle}
                  onClick={() => handleTransformationChange('rotate', null, angle)}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: transformations.rotate === angle ? '#007bff' : '#f8f9fa',
                    color: transformations.rotate === angle ? 'white' : '#333',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  {angle}°
                </button>
              ))}
            </div>
          </div>

          {/* Format Controls */}
          <div>
            <h4>Format Conversion</h4>
            <select
              value={transformations.format}
              onChange={(e) => handleTransformationChange('format', null, e.target.value)}
              style={{ padding: '8px', border: '1px solid #ddd', borderRadius: '4px', width: '100%' }}
            >
              <option value="">Keep original format</option>
              <option value="jpeg">JPEG</option>
              <option value="png">PNG</option>
              <option value="webp">WebP</option>
            </select>
          </div>

          {/* Filter Controls */}
          <div>
            <h4>Filters</h4>
            <div style={{ display: 'flex', gap: '15px' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                <input
                  type="checkbox"
                  checked={transformations.filters.grayscale}
                  onChange={() => handleFilterChange('grayscale')}
                />
                Grayscale
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                <input
                  type="checkbox"
                  checked={transformations.filters.sepia}
                  onChange={() => handleFilterChange('sepia')}
                />
                Sepia
              </label>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '10px', marginTop: '20px', justifyContent: 'flex-end' }}>
          <button
            onClick={resetTransformations}
            style={{
              padding: '10px 20px',
              backgroundColor: '#6c757d',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Reset
          </button>
          <button
            onClick={applyTransformations}
            disabled={loading}
            style={{
              padding: '10px 20px',
              backgroundColor: loading ? '#6c757d' : '#28a745',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Applying...' : 'Apply Transformations'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ImageTransform;