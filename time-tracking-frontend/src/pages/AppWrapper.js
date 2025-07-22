import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LoginPage from '../pages/Login';
import Dashboard from '../components/Dashboard';

export default function AppWrapper() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/admin-dashboard" element={<Dashboard />} />
        <Route path="/employee-dashboard" element={<Dashboard />} />
      </Routes>
    </Router>
  );
}
