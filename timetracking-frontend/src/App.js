import React from 'react';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import AdminDashboard from './components/admin/AdminDashboard';
import EmployeeManagement from './components/admin/EmployeeManagement';
import PayrollProcessing from './components/admin/PayrollProcessing';
import TimeLogs from './components/admin/TimeLogs';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/employees" element={<EmployeeManagement />} />
        <Route path="/admin/timelogs" element={<TimeLogs />} />
        <Route path="/admin/payroll" element={<PayrollProcessing />} />
      </Routes>
    </Router>
  );
}

export default App;
