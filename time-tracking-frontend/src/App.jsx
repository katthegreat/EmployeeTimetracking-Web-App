import { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';

// Frontend user database
const users = [
  { username: 'admin', password: 'adminpass', role: 'admin', empid: 11 },
  { username: 'snoopy', password: 'emppass', role: 'employee', empid: 1 },
  { username: 'charlie', password: 'emppass', role: 'employee', empid: 2 },
  { username: 'lucy', password: 'emppass', role: 'employee', empid: 3 },
  { username: 'linus', password: 'emppass', role: 'employee', empid: 4 },
  { username: 'patty', password: 'emppass', role: 'employee', empid: 5 },
  { username: 'schroeder', password: 'emppass', role: 'employee', empid: 6 },
  { username: 'woodstock', password: 'emppass', role: 'employee', empid: 7 },
  { username: 'marcie', password: 'emppass', role: 'employee', empid: 8 },
  { username: 'franklin', password: 'emppass', role: 'employee', empid: 9 },
  { username: 'pigpen', password: 'emppass', role: 'employee', empid: 10 }
];

function LoginPage() {
  const [formData, setFormData] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const user = users.find(u => 
      u.username === formData.username && 
      u.password === formData.password
    );

    if (user) {
      localStorage.setItem('auth', JSON.stringify(user));
      navigate(user.role === 'admin' ? '/admin-dashboard' : '/employee-dashboard');
    } else {
      setError('Invalid username or password');
    }
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Employee Login</h2>
      {error && <div style={styles.error}>{error}</div>}
      <form onSubmit={handleSubmit} style={styles.form}>
        <div style={styles.inputGroup}>
          <label>Username:</label>
          <input
            name="username"
            value={formData.username}
            onChange={handleChange}
            required
            style={styles.input}
          />
        </div>
        <div style={styles.inputGroup}>
          <label>Password:</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
            style={styles.input}
          />
        </div>
        <button type="submit" style={styles.button}>Login</button>
      </form>
    </div>
  );
}

const styles = {
  container: { 
    maxWidth: '400px', 
    margin: '2rem auto', 
    padding: '2rem', 
    border: '1px solid #ddd',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
  },
  title: { 
    textAlign: 'center',
    marginBottom: '1.5rem',
    color: '#333'
  },
  form: { 
    display: 'flex', 
    flexDirection: 'column', 
    gap: '1.25rem' 
  },
  inputGroup: { 
    display: 'flex', 
    flexDirection: 'column', 
    gap: '0.5rem' 
  },
  input: { 
    padding: '0.75rem',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '1rem'
  },
  button: { 
    padding: '0.75rem', 
    background: '#646cff', 
    color: 'white', 
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
    fontWeight: '600',
    transition: 'background 0.2s',
    ':hover': {
      background: '#4f56d8'
    }
  },
  error: { 
    color: '#ff3333',
    padding: '0.5rem',
    backgroundColor: '#ffebee',
    borderRadius: '4px',
    textAlign: 'center'
  }
};

function App() {
  const [auth, setAuth] = useState(null);

  useEffect(() => {
    const storedAuth = localStorage.getItem('auth');
    if (storedAuth) setAuth(JSON.parse(storedAuth));
  }, []);

  return (
    <Routes>
      <Route path="/" element={auth ? <Navigate to={auth.role === 'admin' ? '/admin-dashboard' : '/employee-dashboard'} /> : <LoginPage />} />
      <Route path="/admin-dashboard" element={auth?.role === 'admin' ? <Dashboard role="admin" /> : <Navigate to="/" />} />
      <Route path="/employee-dashboard" element={auth?.role === 'employee' ? <Dashboard role="employee" /> : <Navigate to="/" />} />
    </Routes>
  );
}

export default App;
export { LoginPage };