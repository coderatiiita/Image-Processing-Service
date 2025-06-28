import React, { useState } from 'react';
import api from '../config/api';

function Login({ onLogin, onSwitchToRegister }) {
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const login = async () => {
    if (!credentials.username || !credentials.password) {
      setMessage('Please enter both username and password');
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/login', credentials);
      localStorage.setItem('token', res.data);
      setMessage('Login successful!');
      onLogin(res.data);
    } catch (error) {
      setMessage('Login failed: ' + (error.response?.data || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      login();
    }
  };

  return (
    <div>
      <h2>Login</h2>
      
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
      </div>
      
      <button 
        onClick={login}
        disabled={loading}
        style={{ 
          padding: '10px 20px', 
          marginRight: '10px', 
          backgroundColor: loading ? '#6c757d' : '#007bff', 
          color: 'white', 
          border: 'none', 
          borderRadius: '4px',
          cursor: loading ? 'not-allowed' : 'pointer'
        }}
      >
        {loading ? 'Logging in...' : 'Login'}
      </button>
      
      <button 
        onClick={onSwitchToRegister}
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
        Switch to Register
      </button>
    </div>
  );
}

export default Login;