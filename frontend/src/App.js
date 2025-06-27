import React, { useState } from 'react';
import axios from 'axios';

function App() {
  const [token, setToken] = useState('');
  const [file, setFile] = useState(null);
  const [credentials, setCredentials] = useState({ username: '', password: '' });

  const login = async () => {
    const res = await axios.post('/login', credentials);
    setToken(res.data);
  };

  const upload = async () => {
    const form = new FormData();
    form.append('file', file);
    const res = await axios.post('/images', form, { headers: { Authorization: `Bearer ${token}` } });
    alert(res.data);
  };

  return (
    <div>
      <h1>Image Service</h1>
      <input placeholder="username" onChange={e => setCredentials({ ...credentials, username: e.target.value })} />
      <input type="password" placeholder="password" onChange={e => setCredentials({ ...credentials, password: e.target.value })} />
      <button onClick={login}>Login</button>
      <br />
      <input type="file" onChange={e => setFile(e.target.files[0])} />
      <button onClick={upload}>Upload</button>
    </div>
  );
}

export default App;
