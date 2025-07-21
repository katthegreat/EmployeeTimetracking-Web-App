import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LoginPage from './App';
import Dashboard from './pages/Dashboard';

function AppWrapper() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/admin-dashboard" element={<Dashboard role="admin" />} />
        <Route path="/employee-dashboard" element={<Dashboard role="employee" />} />
      </Routes>
    </Router>
  );
}

export default AppWrapper;