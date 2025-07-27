-- =========================================
-- Detailed Payroll Report for All Employees
-- Includes: Name, Clock In/Out, Hours, Pay, Tax, 401k, Net Pay
-- =========================================
USE timetracking;

SELECT 
    e.empid,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    DATE(t.punch_in) AS date,
    TIME(t.punch_in) AS clock_in,
    TIME(t.punch_out) AS clock_out,
    ROUND(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out) / 3600, 2) AS hours_worked,
    e.hourly_rate,
    ROUND((TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out) / 3600) * e.hourly_rate, 2) AS gross_pay,
    ROUND((TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out) / 3600) * e.hourly_rate * 0.10, 2) AS tax_deduction,
    ROUND((TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out) / 3600) * e.hourly_rate * 0.05, 2) AS contribution_401k,
    ROUND((TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out) / 3600) * e.hourly_rate * 0.85, 2) AS net_pay
FROM time_logs t
JOIN employees e ON t.empid = e.empid
WHERE t.punch_out IS NOT NULL
ORDER BY e.empid, t.punch_in DESC;
