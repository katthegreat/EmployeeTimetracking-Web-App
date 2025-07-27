-- Create the database and use it
CREATE DATABASE IF NOT EXISTS timetracking;
USE timetracking;

-- Disable FK checks during creation
SET FOREIGN_KEY_CHECKS=0;

-- Drop tables if they exist (to avoid conflicts)
DROP TABLE IF EXISTS employee_reports;
DROP TABLE IF EXISTS employee_action_log;
DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS time_logs;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS employees;

-- EMPLOYEES (with timestamps and validation)
CREATE TABLE employees (
  empid INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL,
  email VARCHAR(100),
  hire_date DATE,
  is_active BOOLEAN DEFAULT TRUE,
  hourly_rate DECIMAL(6,2) NOT NULL,
  job_title VARCHAR(100) DEFAULT 'Unassigned',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_valid_email CHECK (email IS NULL OR email LIKE '%_@__%.__%'),
  CONSTRAINT chk_hourly_rate CHECK (hourly_rate > 0)
);

-- USERS (with plain text passwords as requested)
CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL, -- Plain text storage
  role ENUM('admin','employee') NOT NULL,
  empid INT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (empid) REFERENCES employees(empid)
);

-- TIME LOGS (with validation)
CREATE TABLE time_logs (
  time_log_id INT AUTO_INCREMENT PRIMARY KEY,
  empid INT NOT NULL,
  punch_in DATETIME NOT NULL,
  punch_out DATETIME,
  notes VARCHAR(255),
  FOREIGN KEY (empid) REFERENCES employees(empid),
  CONSTRAINT chk_punch_order CHECK (punch_out IS NULL OR punch_in < punch_out)
);

-- USER SESSIONS (with validation)
CREATE TABLE user_sessions (
  session_id INT AUTO_INCREMENT PRIMARY KEY,
  empid INT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  FOREIGN KEY (empid) REFERENCES employees(empid),
  CONSTRAINT chk_session_expiry CHECK (expires_at > created_at)
);

-- PAYROLL
CREATE TABLE payroll (
  payroll_id INT AUTO_INCREMENT PRIMARY KEY,
  empid INT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  total_hours DECIMAL(6,2) NOT NULL,
  total_pay DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (empid) REFERENCES employees(empid),
  CONSTRAINT chk_payroll_period CHECK (period_end >= period_start)
);

-- EMPLOYEE ACTION LOG
CREATE TABLE employee_action_log (
  action_id INT AUTO_INCREMENT PRIMARY KEY,
  admin_user_id INT NOT NULL,
  empid INT NOT NULL,
  action_type ENUM('create','update','delete') NOT NULL,
  action_details VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (admin_user_id) REFERENCES users(user_id),
  FOREIGN KEY (empid) REFERENCES employees(empid)
);

-- EMPLOYEE REPORTS
CREATE TABLE employee_reports (
  report_id INT AUTO_INCREMENT PRIMARY KEY,
  admin_user_id INT NOT NULL,
  empid INT NOT NULL,
  report_title VARCHAR(100) NOT NULL,
  report_details TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (admin_user_id) REFERENCES users(user_id),
  FOREIGN KEY (empid) REFERENCES employees(empid)
);

-- Indexes for performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_empid ON users(empid);
CREATE INDEX idx_time_logs_empid ON time_logs(empid);
CREATE INDEX idx_time_logs_dates ON time_logs(punch_in, punch_out);
CREATE INDEX idx_user_sessions_token ON user_sessions(token);
CREATE INDEX idx_employee_active ON employees(is_active);

-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS=1;


