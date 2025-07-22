import { useRouter } from 'next/router';
import { useEffect } from 'react';

export default function LoginPage() {
  const router = useRouter();

  const handleLogin = (role) => {
    localStorage.setItem('auth', JSON.stringify({
      username: role === 'admin' ? 'admin' : 'snoopy',
      role,
      empid: role === 'admin' ? 11 : 1
    }));

    router.push(role === 'admin' ? '/admin-dashboard' : '/employee-dashboard');
  };

  return (
    <div style={{ textAlign: 'center', marginTop: '3rem' }}>
      <h2>Employee Login</h2>
      <button onClick={() => handleLogin('admin')}>Login as Admin</button>
      <button onClick={() => handleLogin('employee')}>Login as Employee</button>
    </div>
  );
}
