import React, { useState } from 'react';
import axios from 'axios';

function Register({ onRegister, onSwitchToLogin }) {
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const register = async () => {
    if (!credentials.username || !credentials.password || !confirmPassword) {
      setMessage('Please fill in all fields');
      return;
    }

    if (credentials.password !== confirmPassword) {
      setMessage('Passwords do not match');
      return;
    }

    if (credentials.password.length < 6) {
      setMessage('Password must be at least 6 characters long');
      return;
    }

    setLoading(true);
    try {
      const res = await axios.post('/register', credentials);
      localStorage.setItem('token', res.data);
      setMessage('Registration successful!');
      onRegister(res.data);
    } catch (error) {
      setMessage('Registration failed: ' + (error.response?.data || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      register();
    }
  };

  return (
    <div>
      <h2>Register</h2>
      
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

      <div style={{ marginBottom: '10px' }}>
        <input 
          placeholder="Username" 
          value={credentials.username}
          onChange={e => setCredentials({ ...credentials, username: e.target.value })}
          onKeyPress={handleKeyPress}
          style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
          disabled={loading}
        />
        <input 
          type="password" 
          placeholder="Password" 
          value={credentials.password}
          onChange={e => setCredentials({ ...credentials, password: e.target.value })}
          onKeyPress={handleKeyPress}
          style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
          disabled={loading}
        />
        <input 
          type="password" 
          placeholder="Confirm Password" 
          value={confirmPassword}
          onChange={e => setConfirmPassword(e.target.value)}
          onKeyPress={handleKeyPress}
          style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
          disabled={loading}
        />
      </div>
      
      <button 
        onClick={register}
        disabled={loading}
        style={{ 
          padding: '10px 20px', 
          marginRight: '10px', 
          backgroundColor: loading ? '#6c757d' : '#28a745', 
          color: 'white', 
          border: 'none', 
          borderRadius: '4px',
          cursor: loading ? 'not-allowed' : 'pointer'
        }}
      >
        {loading ? 'Registering...' : 'Register'}
      </button>
      
      <button 
        onClick={onSwitchToLogin}
        disabled={loading}
        style={{ 
          padding: '10px 20px', 
          backgroundColor: '#6c757d', 
          color: 'white', 
          border: 'none', 
          borderRadius: '4px',
          cursor: loading ? 'not-allowed' : 'pointer'
        }}
      >
        Switch to Login
      </button>
    </div>
  );
}

export default Register;