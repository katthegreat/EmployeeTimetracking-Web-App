import React, { useState, useEffect } from 'react';
import axios from 'axios';

export default function AdminControls() {
  const [empid, setEmpid] = useState('');
  const [hourlyRate, setHourlyRate] = useState('');
  const [jobTitle, setJobTitle] = useState('');
  const [employees, setEmployees] = useState([]);
  const [search, setSearch] = useState('');
  const [payrollReport, setPayrollReport] = useState([]);
  const [showReport, setShowReport] = useState(false);
  const [newEmployee, setNewEmployee] = useState({
    firstName: '',
    lastName: '',
    email: '',
    hourlyRate: '',
    jobTitle: ''
  });

  const auth = JSON.parse(localStorage.getItem('auth'));
  const token = auth?.token;
  const adminId = auth?.empid;

  useEffect(() => {
    fetchEmployees();
  }, []);

  const fetchEmployees = async () => {
    try {
      const res = await axios.get('http://localhost:4567/api/employees');
      setEmployees(res.data);
    } catch (error) {
      console.error(error);
      alert('Error fetching employee list');
    }
  };

  const handleUpdatePay = async () => {
    try {
      const res = await axios.post('http://localhost:4567/api/employees/update-pay', {
        empid: parseInt(empid),
        hourlyRate: parseFloat(hourlyRate),
        token,
      });
      alert(res.data.success ? 'Pay updated successfully!' : 'Failed to update pay');
    } catch (error) {
      console.error(error);
      alert('Error updating pay');
    }
  };

  const handleUpdateTitle = async () => {
    try {
      const res = await axios.post('http://localhost:4567/api/employees/update-title', {
        empid: parseInt(empid),
        jobTitle,
        token,
      });
      alert(res.data.success ? 'Job title updated successfully!' : 'Failed to update title');
    } catch (error) {
      console.error(error);
      alert('Error updating title');
    }
  };

  const handleAddEmployee = async () => {
    try {
      const res = await axios.post('http://localhost:4567/api/employees', {
        ...newEmployee,
        hourlyRate: parseFloat(newEmployee.hourlyRate),
        token,
        adminId,
      });
      alert(res.data.success ? 'Employee added!' : 'Failed to add employee');
      fetchEmployees();
    } catch (error) {
      console.error(error);
      alert('Error adding employee');
    }
  };

  const handleDeactivateEmployee = async (id) => {
    try {
      const res = await axios.post('http://localhost:4567/api/employees/deactivate', {
        empid: id,
        token,
        adminId,
      });
      alert(res.data.success ? 'Employee deactivated' : 'Failed to deactivate employee');
      fetchEmployees();
    } catch (error) {
      console.error(error);
      alert('Error deactivating employee');
    }
  };

  const fetchPayrollReport = async () => {
    try {
      const res = await axios.get('http://localhost:4567/api/payroll-detailed', {
        params: { adminId, token },
      });
      setPayrollReport(res.data);
      setShowReport(true);
    } catch (error) {
      console.error(error);
      alert('Failed to fetch payroll report');
    }
  };

  const filteredEmployees = employees.filter((emp) => {
    const fullName = `${emp.first_name} ${emp.last_name}`.toLowerCase();
    const jobTitle = emp.job_title?.toLowerCase() || '';
    const idString = String(emp.empid);
    return (
      fullName.includes(search.toLowerCase()) ||
      jobTitle.includes(search.toLowerCase()) ||
      idString.includes(search)
    );
  });

  return (
    <div style={{ padding: '1rem', backgroundColor: '#f9f9f9', borderRadius: '10px', marginTop: '2rem' }}>
      <h3 style={{ fontSize: '1.5rem', marginBottom: '1rem' }}>Admin Controls</h3>

      {/* Update Pay */}
      <div style={{ marginBottom: '1.5rem' }}>
        <h4>Update Employee Pay</h4>
        <input type="text" placeholder="Employee ID" value={empid} onChange={(e) => setEmpid(e.target.value)} style={{ marginRight: '8px' }} />
        <input type="text" placeholder="New Hourly Rate" value={hourlyRate} onChange={(e) => setHourlyRate(e.target.value)} style={{ marginRight: '8px' }} />
        <button onClick={handleUpdatePay}>Update Pay</button>
      </div>

      {/* Update Job Title */}
      <div style={{ marginBottom: '1.5rem' }}>
        <h4>Update Employee Job Title</h4>
        <input type="text" placeholder="Employee ID" value={empid} onChange={(e) => setEmpid(e.target.value)} style={{ marginRight: '8px' }} />
        <input type="text" placeholder="New Job Title" value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} style={{ marginRight: '8px' }} />
        <button onClick={handleUpdateTitle}>Update Job Title</button>
      </div>

      {/* Add New Employee */}
      <div style={{ marginBottom: '2rem' }}>
        <h4>Add New Employee</h4>
        <input placeholder="First Name" onChange={(e) => setNewEmployee({ ...newEmployee, firstName: e.target.value })} style={{ marginRight: '8px' }} />
        <input placeholder="Last Name" onChange={(e) => setNewEmployee({ ...newEmployee, lastName: e.target.value })} style={{ marginRight: '8px' }} />
        <input placeholder="Email" onChange={(e) => setNewEmployee({ ...newEmployee, email: e.target.value })} style={{ marginRight: '8px' }} />
        <input placeholder="Hourly Rate" onChange={(e) => setNewEmployee({ ...newEmployee, hourlyRate: e.target.value })} style={{ marginRight: '8px' }} />
        <input placeholder="Job Title" onChange={(e) => setNewEmployee({ ...newEmployee, jobTitle: e.target.value })} style={{ marginRight: '8px' }} />
        <button onClick={handleAddEmployee}>Add Employee</button>
      </div>

      {/* Search & Deactivate */}
      <div>
        <h4>Search & Deactivate Employees</h4>
        <input
          placeholder="Search by name, job title, or ID"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: '100%', marginBottom: '1rem', padding: '6px' }}
        />
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {filteredEmployees.map(emp => (
            <li key={emp.empid} style={{ backgroundColor: '#fff', padding: '1rem', borderRadius: '8px', marginBottom: '1rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
              <strong>{emp.first_name} {emp.last_name}</strong> — {emp.job_title || '—'}<br />
              💵 <strong>${emp.hourly_rate?.toFixed(2)}</strong> | ⏱️ <strong>{emp.hours_worked ?? 0} hrs/week</strong><br />
              📊 Weekly Pay: <strong>${emp.weekly_pay?.toFixed(2) || '0.00'}</strong> | Monthly: <strong>${emp.monthly_pay?.toFixed(2) || '0.00'}</strong><br />
              <button onClick={() => handleDeactivateEmployee(emp.empid)} style={{ marginTop: '8px' }}>
                Deactivate
              </button>
            </li>
          ))}
        </ul>
      </div>

      {/* Detailed Payroll Report */}
      <div style={{ marginTop: '3rem' }}>
        <h4>All Employees Payroll Report</h4>
        <button onClick={fetchPayrollReport}>View Payroll Report</button>

        {showReport && payrollReport.length > 0 && (
          <table className="summary-table" style={{ width: '100%', marginTop: '1rem', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th>EmpID</th>
                <th>Name</th>
                <th>Date</th>
                <th>Clock In</th>
                <th>Clock Out</th>
                <th>Hours</th>
                <th>Rate</th>
                <th>Gross</th>
                <th>Tax</th>
                <th>401(k)</th>
                <th>Net Pay</th>
              </tr>
            </thead>
            <tbody>
              {payrollReport.map((r, i) => (
                <tr key={i}>
                  <td>{r.empid}</td>
                  <td>{r.employee_name}</td>
                  <td>{r.date}</td>
                  <td>{r.clock_in}</td>
                  <td>{r.clock_out}</td>
                  <td>{r.hours_worked.toFixed(2)}</td>
                  <td>${r.hourly_rate.toFixed(2)}</td>
                  <td>${r.gross_pay.toFixed(2)}</td>
                  <td>${r.tax_deduction.toFixed(2)}</td>
                  <td>${r.contribution_401k.toFixed(2)}</td>
                  <td>${r.net_pay.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
