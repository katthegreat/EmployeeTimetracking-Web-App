import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import axios from 'axios';
import TimeTracker from '../components/TimeTracker';

export default function EmployeeDashboard() {
  const router = useRouter();
  const [auth, setAuth] = useState(null);
  const [status, setStatus] = useState('');
  const [summary, setSummary] = useState({ weekly: null, monthly: null });
  const [detailedReport, setDetailedReport] = useState([]);
  const [clockDate, setClockDate] = useState('');
  const [clockTime, setClockTime] = useState('');

  useEffect(() => {
    const stored = localStorage.getItem('auth');
    if (stored) {
      const parsed = JSON.parse(stored);
      if (parsed.role !== 'employee') {
        alert('Access denied. Employees only.');
        router.push('/login');
      } else {
        setAuth(parsed);
      }
    } else {
      router.push('/login');
    }
  }, []);

  const buildTimestamp = () => {
    if (!clockDate || !clockTime) return null;
    return `${clockDate} ${clockTime}:00`; // Format for Java
  };

  const handleClockIn = async () => {
    const timestamp = buildTimestamp();
    if (!timestamp) return alert('Please select both date and time to clock in.');
    try {
      const res = await axios.post('http://localhost:4567/api/clock-in', {
        empid: auth.empid,
        token: auth.token,
        timestamp
      });
      setStatus(res.data.message || 'Clocked in!');
    } catch (err) {
      console.error(err);
      setStatus(err.response?.data?.error || 'Failed to clock in.');
    }
  };

  const handleClockOut = async () => {
    const timestamp = buildTimestamp();
    if (!timestamp) return alert('Please select both date and time to clock out.');
    try {
      const res = await axios.post('http://localhost:4567/api/clock-out', {
        empid: auth.empid,
        token: auth.token,
        timestamp
      });
      setStatus(res.data.message || 'Clocked out!');
    } catch (err) {
      console.error(err);
      setStatus(err.response?.data?.error || 'Failed to clock out.');
    }
  };

  const fetchSummaries = async () => {
    try {
      const res = await axios.get('http://localhost:4567/api/employee-summary', {
        params: { empid: auth.empid, token: auth.token },
      });
      setSummary(res.data);
    } catch (err) {
      console.error(err);
      setStatus('Could not fetch pay summaries.');
    }
  };

  const fetchDetailedReport = async () => {
    try {
      const res = await axios.get('http://localhost:4567/api/employee-report', {
        params: {
          empid: auth.empid,
          token: auth.token,
          type: 'detailed'
        }
      });
      setDetailedReport(res.data);
    } catch (err) {
      console.error(err);
      setStatus('Could not load detailed report.');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('auth');
    router.push('/login');
  };

  if (!auth) return <p className="centered-text">Loading...</p>;

  return (
    <div className="employee-dashboard">
      <div className="dashboard-header">
        <h2>Welcome, {auth.name || 'Employee'}!</h2>
        <button className="logout-button" onClick={handleLogout}>Logout</button>
      </div>

      <div className="clock-actions">
        <label className="label">
          Select Date:
          <input
            type="date"
            value={clockDate}
            onChange={(e) => setClockDate(e.target.value)}
            className="input"
          />
        </label>
        <label className="label" style={{ marginLeft: '1rem' }}>
          Select Time:
          <input
            type="time"
            value={clockTime}
            onChange={(e) => setClockTime(e.target.value)}
            className="input"
          />
        </label>
      </div>

      <div className="clock-actions" style={{ marginTop: '1rem' }}>
        <button className="clock-button" onClick={handleClockIn}>Clock In</button>
        <button className="clock-button" onClick={handleClockOut}>Clock Out</button>
        {status && <p className="status-message">{status}</p>}
      </div>

      <div className="pay-summary">
        <h3>Your Pay Summary</h3>
        <button className="load-summary-button" onClick={fetchSummaries}>Load Summary</button>
        {summary.weekly && summary.monthly && (
          <table className="summary-table">
            <thead>
              <tr>
                <th>Period</th>
                <th>Total Hours</th>
                <th>Total Pay</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Weekly</td>
                <td>{summary.weekly.total_hours}</td>
                <td>${summary.weekly.total_pay.toFixed(2)}</td>
              </tr>
              <tr>
                <td>Monthly</td>
                <td>{summary.monthly.total_hours}</td>
                <td>${summary.monthly.total_pay.toFixed(2)}</td>
              </tr>
            </tbody>
          </table>
        )}
      </div>

      <div className="detailed-report">
        <h3>Your Detailed Report</h3>
        <button className="load-summary-button" onClick={fetchDetailedReport}>View Details</button>

        {detailedReport.length > 0 && (
          <table className="summary-table" style={{ marginTop: '1rem' }}>
            <thead>
              <tr>
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
              {detailedReport.map((r, i) => (
                <tr key={i}>
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

      <div className="time-tracker-section">
        <TimeTracker />
      </div>
    </div>
  );
}
