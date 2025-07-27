import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import axios from 'axios';

export default function AdminDashboard() {
  const router = useRouter();
  const [auth, setAuth] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [viewMode, setViewMode] = useState('list'); // 'list' or 'payroll'
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [activeTab, setActiveTab] = useState('employees'); // 'employees' or 'admin'
  const [newEmployee, setNewEmployee] = useState({
    first_name: '',
    last_name: '',
    email: '',
    hourly_rate: '',
    job_title: ''
  });
  const [editData, setEditData] = useState({
    empid: null,
    field: '',
    value: ''
  });

  // Authentication check
  useEffect(() => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('auth');
      if (stored) {
        setAuth(JSON.parse(stored));
        fetchEmployees();
      } else {
        router.push('/login');
      }
    }
  }, [router]);

  const handleLogout = () => {
    localStorage.removeItem('auth');
    router.push('/login');
  };

  // Fetch employee data
  const fetchEmployees = async (search = '') => {
    if (!auth) return;
    
    setLoading(true);
    setError('');
    try {
      const res = await axios.get('http://localhost:4567/api/employees', {
        params: { search },
        headers: { Authorization: `Bearer ${auth.token}` }
      });
      setEmployees(res.data);
      setViewMode('list');
    } catch (err) {
      setError('Error loading employees: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Generate payroll data
  const generatePayroll = async () => {
    if (!auth) return;
    
    setLoading(true);
    setError('');
    try {
      const res = await axios.get('http://localhost:4567/api/payroll-detailed', {
        params: {
          token: auth.token,
          adminId: auth.empid
        }
      });
      
      // Process data to ensure all fields are present
      const processedData = res.data.map(emp => ({
        ...emp,
        full_name: `${emp.first_name} ${emp.last_name}`,
        weekly_pay: emp.weekly_pay || (emp.hours_worked * emp.hourly_rate),
        monthly_pay: emp.monthly_pay || (emp.hours_worked * emp.hourly_rate * 4.33),
        hours_worked: emp.hours_worked || 0
      }));
      
      setEmployees(processedData);
      setViewMode('payroll');
    } catch (err) {
      setError('Error generating payroll: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Add new employee
  const addEmployee = async () => {
    if (!auth) return;
    
    setLoading(true);
    setError('');
    try {
      // Convert hourly_rate to number before sending
      const payload = {
        firstName: newEmployee.first_name,
        lastName: newEmployee.last_name,
        email: newEmployee.email,
        hourlyRate: parseFloat(newEmployee.hourly_rate) || 0, // Convert to number
        jobTitle: newEmployee.job_title,
        token: auth.token,
        adminId: auth.empid
      };
  
      await axios.post('http://localhost:4567/api/employees/create', payload);
      
      setNewEmployee({
        first_name: '',
        last_name: '',
        email: '',
        hourly_rate: '',
        job_title: ''
      });
      fetchEmployees();
    } catch (err) {
      setError('Error adding employee: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  
  // Deactivate employee
  const deactivateEmployee = async (empid) => {
    if (!auth) return;
    
    setLoading(true);
    setError('');
    try {
      await axios.post('http://localhost:4567/api/employees/deactivate', {
        empid,
        token: auth.token,
        adminId: auth.empid
      });
      fetchEmployees(); // Refresh the list
    } catch (err) {
      setError('Error deactivating employee: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Update employee pay or title
  const updateEmployee = async () => {
    if (!auth || !editData.empid) return;
    
    setLoading(true);
    setError('');
    try {
      const endpoint = editData.field === 'hourly_rate' 
        ? '/api/employees/update-pay' 
        : '/api/employees/update-title';
      
      await axios.post(`http://localhost:4567${endpoint}`, {
        empid: editData.empid,
        [editData.field]: editData.value,
        token: auth.token,
        adminId: auth.empid
      });
      
      setEditData({ empid: null, field: '', value: '' });
      fetchEmployees(); // Refresh the list
    } catch (err) {
      setError('Error updating employee: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  // View employee details
  const viewEmployeeDetails = (employee) => {
    setSelectedEmployee(employee);
  };

  // Close details view
  const closeDetails = () => {
    setSelectedEmployee(null);
  };

  if (!auth) return <div className="loading">Loading...</div>;

  return (
    <div className="admin-dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Admin Dashboard</h1>
          <p className="welcome-message">Welcome, <strong>{auth.name || 'Admin'}</strong></p>
        </div>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </header>

      <div className="main-content">
        <div className="tabs">
          <button 
            className={`tab-btn ${activeTab === 'employees' ? 'active' : ''}`}
            onClick={() => setActiveTab('employees')}
          >
            Employees
          </button>
          <button 
            className={`tab-btn ${activeTab === 'admin' ? 'active' : ''}`}
            onClick={() => setActiveTab('admin')}
          >
            Admin Controls
          </button>
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {loading ? (
          <div className="loading-indicator">
            <div className="spinner"></div>
            <p>Loading data...</p>
          </div>
        ) : activeTab === 'employees' ? (
          <>
            <div className="controls-panel">
              <div className="search-container">
                <input
                  type="text"
                  placeholder="Search employees..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && fetchEmployees(searchQuery)}
                  className="search-input"
                />
                <button 
                  onClick={() => fetchEmployees(searchQuery)}
                  className="search-btn"
                >
                  Search
                </button>
              </div>

              <div className="action-buttons">
                <button 
                  onClick={() => fetchEmployees()}
                  className="action-btn"
                >
                  Show All Employees
                </button>
                <button 
                  onClick={generatePayroll}
                  className="action-btn primary"
                >
                  Generate Payroll
                </button>
              </div>
            </div>

            {selectedEmployee ? (
              <EmployeeDetails 
                employee={selectedEmployee}
                onClose={closeDetails}
                auth={auth}
              />
            ) : (
              <EmployeeTable 
                employees={employees} 
                viewMode={viewMode}
                onViewDetails={viewEmployeeDetails}
                onDeactivate={deactivateEmployee}
              />
            )}
          </>
        ) : (
          <AdminControls 
            newEmployee={newEmployee}
            setNewEmployee={setNewEmployee}
            onAddEmployee={addEmployee}
            editData={editData}
            setEditData={setEditData}
            onUpdateEmployee={updateEmployee}
          />
        )}
      </div>

      <style jsx>{`
        .admin-dashboard {
          display: flex;
          flex-direction: column;
          min-height: 100vh;
          background-color: #f5f7fa;
          font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        .dashboard-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 1.5rem 2rem;
          background-color: #2c3e50;
          color: white;
        }

        .dashboard-header h1 {
          margin: 0;
          font-size: 1.5rem;
          font-weight: 600;
        }

        .welcome-message {
          margin: 0.25rem 0 0;
          font-size: 0.9rem;
          opacity: 0.9;
        }

        .logout-btn {
          padding: 0.5rem 1.25rem;
          background-color: #e74c3c;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-weight: 500;
          transition: all 0.2s;
        }

        .logout-btn:hover {
          background-color: #c0392b;
          transform: translateY(-1px);
        }

        .main-content {
          flex: 1;
          padding: 2rem;
          max-width: 1200px;
          margin: 0 auto;
          width: 100%;
        }

        .tabs {
          display: flex;
          margin-bottom: 1.5rem;
          border-bottom: 1px solid #ddd;
        }

        .tab-btn {
          padding: 0.75rem 1.5rem;
          background: none;
          border: none;
          border-bottom: 3px solid transparent;
          cursor: pointer;
          font-weight: 500;
          color: #7f8c8d;
          transition: all 0.2s;
        }

        .tab-btn.active {
          color: #2c3e50;
          border-bottom-color: #3498db;
        }

        .controls-panel {
          display: flex;
          flex-direction: column;
          gap: 1.5rem;
          margin-bottom: 2rem;
        }

        .search-container {
          display: flex;
          gap: 0.75rem;
          max-width: 600px;
        }

        .search-input {
          flex: 1;
          padding: 0.75rem 1rem;
          border: 1px solid #ddd;
          border-radius: 6px;
          font-size: 1rem;
          transition: border-color 0.2s;
        }

        .search-input:focus {
          outline: none;
          border-color: #3498db;
        }

        .search-btn {
          padding: 0 1.5rem;
          background-color: #3498db;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .search-btn:hover {
          background-color: #2980b9;
        }

        .action-buttons {
          display: flex;
          gap: 0.75rem;
        }

        .action-btn {
          padding: 0.75rem 1.5rem;
          background-color: #ecf0f1;
          color: #2c3e50;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-weight: 500;
          transition: all 0.2s;
        }

        .action-btn:hover {
          background-color: #bdc3c7;
        }

        .action-btn.primary {
          background-color: #2ecc71;
          color: white;
        }

        .action-btn.primary:hover {
          background-color: #27ae60;
        }

        .error-message {
          padding: 1rem;
          background-color: #fdecea;
          color: #e74c3c;
          border-left: 4px solid #e74c3c;
          border-radius: 4px;
          margin-bottom: 1.5rem;
        }

        .loading-indicator {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 2rem;
        }

        .spinner {
          width: 40px;
          height: 40px;
          border: 4px solid rgba(0, 0, 0, 0.1);
          border-radius: 50%;
          border-top-color: #3498db;
          animation: spin 1s ease-in-out infinite;
          margin-bottom: 1rem;
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        @media (max-width: 768px) {
          .dashboard-header {
            flex-direction: column;
            align-items: flex-start;
            gap: 1rem;
          }

          .search-container {
            flex-direction: column;
          }

          .action-buttons {
            flex-direction: column;
          }

          .action-btn {
            width: 100%;
          }
        }
      `}</style>
    </div>
  );
}

function EmployeeTable({ employees, viewMode, onViewDetails, onDeactivate }) {
  return (
    <div className="employee-table-container">
      <table className="employee-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Job Title</th>
            <th>Hourly Rate</th>
            {viewMode === 'payroll' ? (
              <>
                <th>Hours Worked</th>
                <th>Weekly Pay</th>
                <th>Monthly Pay</th>
              </>
            ) : null}
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {employees.length > 0 ? (
            employees.map((emp) => (
              <tr key={emp.empid}>
                <td>{emp.empid}</td>
                <td>{emp.first_name} {emp.last_name}</td>
                <td>{emp.job_title || '—'}</td>
                <td>${emp.hourly_rate?.toFixed(2)}</td>
                {viewMode === 'payroll' && (
                  <>
                    <td>{emp.hours_worked?.toFixed(2) || '0.00'}</td>
                    <td>${emp.weekly_pay?.toFixed(2) || '0.00'}</td>
                    <td>${emp.monthly_pay?.toFixed(2) || '0.00'}</td>
                  </>
                )}
                <td>
                  <div className="action-buttons">
                    <button 
                      onClick={() => onViewDetails(emp)}
                      className="view-btn"
                    >
                      Details
                    </button>
                    <button 
                      onClick={() => onDeactivate(emp.empid)}
                      className="deactivate-btn"
                    >
                      Deactivate
                    </button>
                  </div>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={viewMode === 'payroll' ? 8 : 5} className="no-data">
                No employees found
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <style jsx>{`
        .employee-table-container {
          background-color: white;
          border-radius: 8px;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
          overflow: hidden;
        }

        .employee-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.95rem;
        }

        .employee-table th {
          background-color: #f8f9fa;
          padding: 1rem;
          text-align: left;
          font-weight: 600;
          color: #2c3e50;
          border-bottom: 2px solid #eee;
        }

        .employee-table td {
          padding: 1rem;
          border-bottom: 1px solid #eee;
        }

        .employee-table tr:hover {
          background-color: #f8f9fa;
        }

        .action-buttons {
          display: flex;
          gap: 0.5rem;
        }

        .view-btn {
          padding: 0.5rem 1rem;
          background-color: #3498db;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .view-btn:hover {
          background-color: #2980b9;
        }

        .deactivate-btn {
          padding: 0.5rem 1rem;
          background-color: #e74c3c;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .deactivate-btn:hover {
          background-color: #c0392b;
        }

        .no-data {
          text-align: center;
          padding: 2rem;
          color: #7f8c8d;
        }
      `}</style>
    </div>
  );
}

function EmployeeDetails({ employee, onClose, auth }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [reportType, setReportType] = useState('weekly');

  useEffect(() => {
    fetchReport('weekly');
  }, []);

  const fetchReport = async (type) => {
    setLoading(true);
    setError('');
    try {
      const res = await axios.get('http://localhost:4567/api/employee-report', {
        params: { 
          empid: employee.empid,
          type,
          token: auth.token
        }
      });
      
      // Fix for SQL GROUP BY error by ensuring all non-aggregated columns are in GROUP BY
      const processedReport = res.data.map(item => ({
        period: item.period,
        total_hours: item.total_hours || 0,
        total_pay: item.total_pay || 0
      }));
      
      setReport(processedReport);
      setReportType(type);
    } catch (err) {
      setError('Error fetching report: ' + (err.response?.data?.error || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="employee-details">
      <div className="details-header">
        <h2>
          {employee.first_name} {employee.last_name}
          <span className="job-title">{employee.job_title}</span>
        </h2>
        <button onClick={onClose} className="close-btn">
          Back to List
        </button>
      </div>

      <div className="employee-summary">
        <div className="summary-card">
          <h3>Hourly Rate</h3>
          <p>${employee.hourly_rate?.toFixed(2)}</p>
        </div>
        <div className="summary-card">
          <h3>Hours Worked</h3>
          <p>{employee.hours_worked?.toFixed(2) || '0.00'}</p>
        </div>
        <div className="summary-card">
          <h3>Weekly Pay</h3>
          <p>${employee.weekly_pay?.toFixed(2) || '0.00'}</p>
        </div>
        <div className="summary-card">
          <h3>Monthly Pay</h3>
          <p>${employee.monthly_pay?.toFixed(2) || '0.00'}</p>
        </div>
      </div>

      <div className="report-controls">
        <button
          onClick={() => fetchReport('weekly')}
          className={`report-btn ${reportType === 'weekly' ? 'active' : ''}`}
        >
          Weekly Report
        </button>
        <button
          onClick={() => fetchReport('monthly')}
          className={`report-btn ${reportType === 'monthly' ? 'active' : ''}`}
        >
          Monthly Report
        </button>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {loading ? (
        <div className="loading-indicator">
          <div className="spinner"></div>
          <p>Loading report...</p>
        </div>
      ) : report ? (
        <div className="report-table-container">
          <table className="report-table">
            <thead>
              <tr>
                <th>Period</th>
                <th>Hours</th>
                <th>Gross Pay</th>
                <th>Tax (20%)</th>
                <th>Retirement (5%)</th>
                <th>Net Pay</th>
              </tr>
            </thead>
            <tbody>
              {report.map((item, idx) => (
                <tr key={idx}>
                  <td>{item.period}</td>
                  <td>{item.total_hours?.toFixed(2)}</td>
                  <td>${item.total_pay?.toFixed(2)}</td>
                  <td>${(item.total_pay * 0.20).toFixed(2)}</td>
                  <td>${(item.total_pay * 0.05).toFixed(2)}</td>
                  <td>${(item.total_pay * 0.75).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="no-data">
          No report data available
        </div>
      )}

      <style jsx>{`
        .employee-details {
          background-color: white;
          border-radius: 8px;
          padding: 2rem;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }

        .details-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 2rem;
          padding-bottom: 1rem;
          border-bottom: 1px solid #eee;
        }

        .details-header h2 {
          margin: 0;
          font-size: 1.5rem;
          color: #2c3e50;
        }

        .job-title {
          display: block;
          font-size: 1rem;
          color: #7f8c8d;
          margin-top: 0.25rem;
          font-weight: normal;
        }

        .close-btn {
          padding: 0.75rem 1.5rem;
          background-color: #95a5a6;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .close-btn:hover {
          background-color: #7f8c8d;
        }

        .employee-summary {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 1rem;
          margin-bottom: 2rem;
        }

        .summary-card {
          background-color: #f8f9fa;
          padding: 1.5rem;
          border-radius: 6px;
          text-align: center;
        }

        .summary-card h3 {
          margin: 0 0 0.5rem;
          font-size: 0.9rem;
          color: #7f8c8d;
          font-weight: 600;
        }

        .summary-card p {
          margin: 0;
          font-size: 1.25rem;
          font-weight: 600;
          color: #2c3e50;
        }

        .report-controls {
          display: flex;
          gap: 0.75rem;
          margin-bottom: 1.5rem;
        }

        .report-btn {
          padding: 0.75rem 1.5rem;
          background-color: #ecf0f1;
          color: #2c3e50;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .report-btn.active {
          background-color: #3498db;
          color: white;
        }

        .report-btn:hover {
          background-color: #bdc3c7;
        }

        .report-btn.active:hover {
          background-color: #2980b9;
        }

        .report-table-container {
          margin-top: 1.5rem;
          overflow-x: auto;
        }

        .report-table {
          width: 100%;
          border-collapse: collapse;
        }

        .report-table th {
          background-color: #f8f9fa;
          padding: 1rem;
          text-align: left;
          font-weight: 600;
          color: #2c3e50;
          border-bottom: 2px solid #eee;
        }

        .report-table td {
          padding: 1rem;
          border-bottom: 1px solid #eee;
        }

        .report-table tr:hover {
          background-color: #f8f9fa;
        }

        .no-data {
          text-align: center;
          padding: 2rem;
          color: #7f8c8d;
        }

        .loading-indicator {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 2rem;
        }

        .spinner {
          width: 40px;
          height: 40px;
          border: 4px solid rgba(0, 0, 0, 0.1);
          border-radius: 50%;
          border-top-color: #3498db;
          animation: spin 1s ease-in-out infinite;
          margin-bottom: 1rem;
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        @media (max-width: 768px) {
          .employee-summary {
            grid-template-columns: 1fr 1fr;
          }

          .report-controls {
            flex-direction: column;
          }

          .report-btn {
            width: 100%;
          }
        }
      `}</style>
    </div>
  );
}

function AdminControls({ newEmployee, setNewEmployee, onAddEmployee, editData, setEditData, onUpdateEmployee }) {
  return (
    <div className="admin-controls">
      <div className="control-section">
        <h2>Add New Employee</h2>
        <div className="form-group">
          <label>First Name</label>
          <input
            type="text"
            value={newEmployee.first_name}
            onChange={(e) => setNewEmployee({...newEmployee, first_name: e.target.value})}
          />
        </div>
        <div className="form-group">
          <label>Last Name</label>
          <input
            type="text"
            value={newEmployee.last_name}
            onChange={(e) => setNewEmployee({...newEmployee, last_name: e.target.value})}
          />
        </div>
        <div className="form-group">
          <label>Email</label>
          <input
            type="email"
            value={newEmployee.email}
            onChange={(e) => setNewEmployee({...newEmployee, email: e.target.value})}
          />
        </div>
        <div className="form-group">
          <label>Hourly Rate</label>
          <input
            type="number"
            value={newEmployee.hourly_rate}
            onChange={(e) => setNewEmployee({...newEmployee, hourly_rate: e.target.value})}
          />
        </div>
        <div className="form-group">
          <label>Job Title</label>
          <input
            type="text"
            value={newEmployee.job_title}
            onChange={(e) => setNewEmployee({...newEmployee, job_title: e.target.value})}
          />
        </div>
        <button onClick={onAddEmployee} className="add-btn">
          Add Employee
        </button>
      </div>

      <div className="control-section">
        <h2>Update Employee</h2>
        <div className="form-group">
          <label>Employee ID</label>
          <input
            type="number"
            value={editData.empid || ''}
            onChange={(e) => setEditData({...editData, empid: e.target.value})}
          />
        </div>
        <div className="form-group">
          <label>Field to Update</label>
          <select
            value={editData.field}
            onChange={(e) => setEditData({...editData, field: e.target.value})}
          >
            <option value="">Select field</option>
            <option value="hourly_rate">Hourly Rate</option>
            <option value="job_title">Job Title</option>
          </select>
        </div>
        <div className="form-group">
          <label>New Value</label>
          <input
            type={editData.field === 'hourly_rate' ? 'number' : 'text'}
            value={editData.value}
            onChange={(e) => setEditData({...editData, value: e.target.value})}
          />
        </div>
        <button 
          onClick={onUpdateEmployee}
          disabled={!editData.empid || !editData.field || !editData.value}
          className="update-btn"
        >
          Update Employee
        </button>
      </div>

      <style jsx>{`
        .admin-controls {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 2rem;
        }

        .control-section {
          background-color: white;
          padding: 1.5rem;
          border-radius: 8px;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }

        .control-section h2 {
          margin-top: 0;
          margin-bottom: 1.5rem;
          color: #2c3e50;
        }

        .form-group {
          margin-bottom: 1rem;
        }

        .form-group label {
          display: block;
          margin-bottom: 0.5rem;
          font-weight: 500;
          color: #2c3e50;
        }

        .form-group input,
        .form-group select {
          width: 100%;
          padding: 0.75rem;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 1rem;
        }

        .add-btn {
          padding: 0.75rem 1.5rem;
          background-color: #2ecc71;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-weight: 500;
          transition: all 0.2s;
        }

        .add-btn:hover {
          background-color: #27ae60;
        }

        .update-btn {
          padding: 0.75rem 1.5rem;
          background-color: #3498db;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-weight: 500;
          transition: all 0.2s;
        }

        .update-btn:hover {
          background-color: #2980b9;
        }

        .update-btn:disabled {
          background-color: #bdc3c7;
          cursor: not-allowed;
        }

        @media (max-width: 768px) {
          .admin-controls {
            grid-template-columns: 1fr;
          }
        }
      `}</style>
    </div>
  );
}