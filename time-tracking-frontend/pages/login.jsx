import React, { useState } from 'react';
import axios from 'axios';
import { useRouter } from 'next/router';

export default function LoginPage() {
  const [formData, setFormData] = useState({ username: '', password: '' });
  const router = useRouter();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post('http://localhost:4567/api/login', formData);
      const { token, role, empid, name } = res.data;

      localStorage.setItem('auth', JSON.stringify({ token, role, empid, name }));

      if (role === 'admin') {
        router.push('/AdminDashboard');
      } else {
        router.push('/EmployeeDashboard');
      }
    } catch (err) {
      console.error(err);
      alert('Login failed — invalid credentials');
    }
  };

  return (
    <div className="login-container">
      <form className="login-form" onSubmit={handleSubmit}>
        <h2 className="login-title">Login</h2>
        <input
          className="login-input"
          type="text"
          name="username"
          placeholder="Username"
          value={formData.username}
          onChange={handleChange}
          required
        />
        <input
          className="login-input"
          type="password"
          name="password"
          placeholder="Password"
          value={formData.password}
          onChange={handleChange}
          required
        />
        <button type="submit" className="button">Login</button>
      </form>
    </div>
  );
}
