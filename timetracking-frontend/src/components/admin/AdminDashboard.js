import React from 'react';
import { Link } from 'react-router-dom';

function AdminDashboard() {
  return (
    <div>
      <h2>Admin Dashboard</h2>
      <ul>
        <li><Link to="/admin/employees">Manage Employees</Link></li>
        <li><Link to="/admin/timelogs">View Time Logs</Link></li>
        <li><Link to="/admin/payroll">Process Payroll</Link></li>
      </ul>
    </div>
  );
}

export default AdminDashboard;
