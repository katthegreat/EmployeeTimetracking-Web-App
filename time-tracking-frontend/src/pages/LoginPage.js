import React from 'react';
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const navigate = useNavigate();
  
  const handleLogin = (role) => {
    localStorage.setItem('auth', JSON.stringify({
      username: role === 'admin' ? 'admin' : 'snoopy',
      role,
      empid: role === 'admin' ? 11 : 1
    }));
    navigate(role === 'admin' ? '/admin-dashboard' : '/employee-dashboard');
  };

  return (
    <div>
      <h2>Employee Login</h2>
      <button onClick={() => handleLogin('admin')}>Login as Admin</button>
      <button onClick={() => handleLogin('employee')}>Login as Employee</button>
    </div>
  );
}

export default LoginPage;