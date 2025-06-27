import React, { useState, useEffect } from 'react';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';

function App() {
  const [token, setToken] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);

  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    if (savedToken) {
      setToken(savedToken);
    }
  }, []);

  const handleLogin = (newToken) => {
    setToken(newToken);
  };

  const handleRegister = (newToken) => {
    setToken(newToken);
    setIsRegistering(false);
  };

  const handleLogout = () => {
    setToken('');
  };

  const switchToRegister = () => {
    setIsRegistering(true);
  };

  const switchToLogin = () => {
    setIsRegistering(false);
  };

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <h1>Image Processing Service</h1>
      
      {!token ? (
        isRegistering ? (
          <Register 
            onRegister={handleRegister} 
            onSwitchToLogin={switchToLogin} 
          />
        ) : (
          <Login 
            onLogin={handleLogin} 
            onSwitchToRegister={switchToRegister} 
          />
        )
      ) : (
        <Dashboard 
          token={token} 
          onLogout={handleLogout} 
        />
      )}
    </div>
  );
}

export default App;
