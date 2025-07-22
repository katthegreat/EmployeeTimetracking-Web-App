import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AdminControls from '../components/AdminControls.jsx';
import TimeTracker from '../components/TimeTracker.jsx';

export default function Dashboard() {
  const navigate = useNavigate();
  const [auth, setAuth] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('auth')) || null;
    } catch (error) {
      console.error('Failed to parse auth data:', error);
      return null;
    }
  });

  const [timeRecords, setTimeRecords] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!auth) {
      navigate('/');
      return;
    }

    if (auth.role !== 'admin') {
      const records = JSON.parse(localStorage.getItem(`timeRecords_${auth.empid}`)) || [];
      setTimeRecords(records);
    }

    setIsLoading(false);
  }, [auth, navigate]);

  const handleLogout = () => {
    localStorage.removeItem('auth');
    setAuth(null);
    navigate('/');
  };

  if (isLoading) {
    return (
      <div style={styles.loadingContainer}>
        <div style={styles.loadingSpinner}></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  if (!auth) return null;

  const isAdmin = auth.role === 'admin';

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <div style={styles.headerLeft}>
          <h1 style={styles.welcomeMessage}>
            Welcome, <span style={styles.username}>{auth.username}</span>!
            {isAdmin && <span style={styles.adminBadge}> (Admin)</span>}
          </h1>
          <p style={styles.lastLogin}>
            Last login: {new Date().toLocaleDateString()} at {new Date().toLocaleTimeString()}
          </p>
        </div>
        <button onClick={handleLogout} style={styles.logoutButton}>
          <span style={styles.logoutIcon}>🚪</span> Logout
        </button>
      </header>

      <div style={styles.content}>
        {isAdmin ? (
          <AdminControls />
        ) : (
          <TimeTracker empid={auth.empid} timeRecords={timeRecords} />
        )}
      </div>

      <footer style={styles.footer}>
        <p>© {new Date().getFullYear()} Time Tracking App</p>
      </footer>
    </div>
  );
}

const styles = {
  container: { display: 'flex', flexDirection: 'column', minHeight: '100vh', padding: '2rem', maxWidth: '1200px', margin: '0 auto', backgroundColor: '#f5f5f5' },
  loadingContainer: { display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh' },
  loadingSpinner: { border: '4px solid rgba(0, 0, 0, 0.1)', borderLeftColor: '#646cff', borderRadius: '50%', width: '40px', height: '40px', animation: 'spin 1s linear infinite', marginBottom: '1rem' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem', paddingBottom: '1rem', borderBottom: '1px solid #ddd' },
  headerLeft: { flex: 1 },
  welcomeMessage: { margin: 0, fontSize: '1.8rem', color: '#333' },
  username: { color: '#646cff', fontWeight: 'bold' },
  adminBadge: { fontSize: '1rem', color: '#ff4444', fontWeight: 'bold', marginLeft: '0.5rem' },
  lastLogin: { margin: '0.25rem 0 0', fontSize: '0.9rem', color: '#666' },
  content: { flex: 1, backgroundColor: '#fff', padding: '1.5rem', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' },
  logoutButton: { display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem', backgroundColor: '#ff4444', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' },
  logoutIcon: { fontSize: '1rem' },
  footer: { marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid #ddd', textAlign: 'center', color: '#666', fontSize: '0.9rem' }
};
