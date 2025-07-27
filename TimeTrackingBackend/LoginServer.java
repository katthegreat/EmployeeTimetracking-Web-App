package TimeTrackingBackend;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import spark.Request;
import spark.Response;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import io.github.cdimascio.dotenv.Dotenv;

public class LoginServer {
    private static final Gson gson = new Gson();
    private static Dotenv dotenv;

    public static void main(String[] args) {
        port(4567);
        dotenv = Dotenv.configure().directory("lib").ignoreIfMissing().load();
        enableCORS();
        setupRoutes();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void enableCORS() {
        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null) res.header("Access-Control-Allow-Headers", headers);
            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
            res.type("application/json");
        });
    }

    private static void setupRoutes() {
        post("/api/login", LoginServer::handleLogin);
        post("/api/logout", LoginServer::handleLogout);
        post("/api/clock-in", LoginServer::handleClockIn);       // ✅ Required
        post("/api/clock-out", LoginServer::handleClockOut);     // ✅ Required
        get("/api/employees", LoginServer::handleGetEmployees);
        post("/api/employees/create", LoginServer::handleCreateEmployee);
        post("/api/employees/deactivate", LoginServer::handleDeactivateEmployee);
        post("/api/employees/update-pay", LoginServer::handleUpdatePay);
        post("/api/employees/update-title", LoginServer::handleUpdateTitle);
        get("/api/employee-summary", LoginServer::handleEmployeeSummary);  // ✅ Required
        get("/api/employee-report", LoginServer::handleEmployeeReport);
        get("/api/clock-status", LoginServer::handleClockStatus);
        get("/api/weekly-summary", LoginServer::handleWeeklySummary);
        get("/api/payroll-detailed", LoginServer::handleAllEmployeePayrollDetailed);

    
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace(); // For debugging
            res.status(500);
            res.body(gson.toJson(Map.of("error", e.getMessage())));
        });
    
        notFound((req, res) -> {
            res.type("application/json");
            return gson.toJson(Map.of("error", "Endpoint not found"));
        });
    }
    private static Object handleLogin(Request req, Response res) throws SQLException {
        Map<String, String> loginRequest = gson.fromJson(req.body(), new TypeToken<Map<String, String>>(){}.getType());
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        try (Connection conn = getConnection()) {
            String sql = "SELECT u.*, e.first_name, e.last_name FROM users u " +
                         "JOIN employees e ON u.empid = e.empid " +
                         "WHERE u.username = ? AND u.password = ? AND e.is_active = TRUE";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String token = UUID.randomUUID().toString();
                    storeSession(conn, rs.getInt("empid"), token);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("token", token);
                    response.put("role", rs.getString("role"));
                    response.put("empid", rs.getInt("empid"));
                    response.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));

                    return gson.toJson(response);
                } else {
                    res.status(401);
                    return gson.toJson(Map.of("error", "Invalid credentials"));
                }
            }
        }
    }

    private static Object handleEmployeeSummary(Request req, Response res) throws SQLException {
        int empid = Integer.parseInt(req.queryParams("empid"));
        String token = req.queryParams("token");
    
        if (!validateSession(empid, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }
    
        try (Connection conn = getConnection()) {
            String sql = "SELECT " +
                         "ROUND(SUM(CASE WHEN punch_in >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN TIMESTAMPDIFF(SECOND, punch_in, punch_out) ELSE 0 END)/3600, 2) as hours_week, " +
                         "ROUND(SUM(CASE WHEN punch_in >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN TIMESTAMPDIFF(SECOND, punch_in, punch_out) ELSE 0 END)/3600, 2) as hours_month, " +
                         "e.hourly_rate " +
                         "FROM employees e LEFT JOIN time_logs t ON e.empid = t.empid AND t.punch_out IS NOT NULL " +
                         "WHERE e.empid = ? " +
                         "GROUP BY e.hourly_rate";
    
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                ResultSet rs = stmt.executeQuery();
    
                if (rs.next()) {
                    double rate = rs.getDouble("hourly_rate");
                    double hoursWeek = rs.getDouble("hours_week");
                    double hoursMonth = rs.getDouble("hours_month");
    
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("weekly", Map.of(
                        "total_hours", hoursWeek,
                        "total_pay", Math.round(hoursWeek * rate * 100.0) / 100.0
                    ));
                    summary.put("monthly", Map.of(
                        "total_hours", hoursMonth,
                        "total_pay", Math.round(hoursMonth * rate * 100.0) / 100.0
                    ));
    
                    return gson.toJson(summary);
                } else {
                    return gson.toJson(Map.of("weekly", Map.of(), "monthly", Map.of()));
                }
            }
        }
    }
    

    private static Object handleClockIn(Request req, Response res) throws SQLException {
        Map<String, Object> request = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());
        int empid = ((Double) request.get("empid")).intValue();
        String token = (String) request.get("token");
    
        if (!validateSession(empid, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }
    
        String timestamp = (String) request.get("timestamp");
        String notes = (String) request.getOrDefault("notes", "");
    
        System.out.println("⏰ Clock-in requested for empid=" + empid + " at " + timestamp);
    
        try (Connection conn = getConnection()) {
            if (isClockedIn(conn, empid)) {
                res.status(400);
                return gson.toJson(Map.of("error", "Already clocked in"));
            }
    
            String sql = "INSERT INTO time_logs (empid, punch_in, notes) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                try {
                    if (timestamp != null && !timestamp.isEmpty()) {
                        stmt.setTimestamp(2, Timestamp.valueOf(timestamp)); // ✅ Expects "YYYY-MM-DD HH:MM:SS"
                    } else {
                        stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    }
                } catch (IllegalArgumentException e) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "Invalid timestamp format. Expected 'YYYY-MM-DD HH:MM:SS'"));
                }
                stmt.setString(3, notes);
                stmt.executeUpdate();
    
                return gson.toJson(Map.of(
                    "success", true,
                    "action", "clock-in",
                    "message", "Clock-in recorded successfully"
                ));
            }
        } catch (SQLException e) {
            res.status(500);
            return gson.toJson(Map.of("error", "Database error: " + e.getMessage()));
        }
    }
    



   

    private static Object handleEmployeeReport(Request req, Response res) throws SQLException {
        int empid = Integer.parseInt(req.queryParams("empid"));
        String type = req.queryParams("type");
    
        try (Connection conn = getConnection()) {
            // First get the employee's hourly rate
            double hourlyRate = 0;
            try (PreparedStatement rateStmt = conn.prepareStatement(
                    "SELECT hourly_rate FROM employees WHERE empid = ?")) {
                rateStmt.setInt(1, empid);
                ResultSet rs = rateStmt.executeQuery();
                if (rs.next()) {
                    hourlyRate = rs.getDouble("hourly_rate");
                }
            }
    
            String sql;
            if ("weekly".equalsIgnoreCase(type)) {
                sql = "SELECT " +
                      "CONCAT(YEAR(punch_in), '-W', LPAD(WEEK(punch_in), 2, '0')) AS period, " +
                      "SUM(TIMESTAMPDIFF(SECOND, punch_in, punch_out)) AS total_seconds " +
                      "FROM time_logs " +
                      "WHERE empid = ? AND punch_out IS NOT NULL " +
                      "GROUP BY YEAR(punch_in), WEEK(punch_in) " +
                      "ORDER BY YEAR(punch_in) DESC, WEEK(punch_in) DESC";
            } else {
                sql = "SELECT " +
                      "DATE_FORMAT(punch_in, '%Y-%m') AS period, " +
                      "SUM(TIMESTAMPDIFF(SECOND, punch_in, punch_out)) AS total_seconds " +
                      "FROM time_logs " +
                      "WHERE empid = ? AND punch_out IS NOT NULL " +
                      "GROUP BY YEAR(punch_in), MONTH(punch_in) " +
                      "ORDER BY YEAR(punch_in) DESC, MONTH(punch_in) DESC";
            }
    
            // Process the report
            List<Map<String, Object>> report = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                ResultSet rs = stmt.executeQuery();
    
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("period", rs.getString("period"));
                    
                    double hours = rs.getLong("total_seconds") / 3600.0;
                    entry.put("total_hours", Math.round(hours * 100.0) / 100.0);
                    entry.put("total_pay", Math.round(hours * hourlyRate * 100.0) / 100.0);
                    
                    report.add(entry);
                }
            }
    
            return gson.toJson(report);
    
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }






    private static Object handleClockOut(Request req, Response res) throws SQLException {
        Map<String, Object> request = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        int empid = ((Double) request.get("empid")).intValue();
        String token = (String) request.get("token");

        if (!validateSession(empid, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }

        String timestamp = (String) request.get("timestamp");

        try (Connection conn = getConnection()) {
            String sql = "UPDATE time_logs SET punch_out = ? WHERE empid = ? AND punch_out IS NULL ORDER BY punch_in DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (timestamp != null && !timestamp.isEmpty()) {
                    stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
                } else {
                    stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                }
                stmt.setInt(2, empid);
                int updated = stmt.executeUpdate();

                if (updated == 0) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "No active clock-in found"));
                }

                return gson.toJson(Map.of("success", true, "action", "clock-out"));
            }
        }
    }

    private static Object handleClockStatus(Request req, Response res) throws SQLException {
        int empid = Integer.parseInt(req.queryParams("empid"));
        String token = req.queryParams("token");

        if (!validateSession(empid, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }

        try (Connection conn = getConnection()) {
            boolean isClockedIn = isClockedIn(conn, empid);
            return gson.toJson(Map.of("isClockedIn", isClockedIn, "empid", empid));
        }
    }

    private static Object handleWeeklySummary(Request req, Response res) throws SQLException {
        int empid = Integer.parseInt(req.queryParams("empid"));
        String token = req.queryParams("token");

        if (!validateSession(empid, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }

        try (Connection conn = getConnection()) {
            String sql = "SELECT DATE(punch_in) as date, " +
                         "SEC_TO_TIME(SUM(TIMESTAMPDIFF(SECOND, punch_in, punch_out))) as total, " +
                         "COUNT(*) as entries " +
                         "FROM time_logs " +
                         "WHERE empid = ? AND punch_out IS NOT NULL " +
                         "AND punch_in >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY) " +
                         "GROUP BY DATE(punch_in) " +
                         "ORDER BY date";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                ResultSet rs = stmt.executeQuery();

                List<Map<String, Object>> summary = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", rs.getString("date"));
                    entry.put("total_hours", rs.getString("total"));
                    entry.put("entries", rs.getInt("entries"));
                    summary.add(entry);
                }
                return gson.toJson(summary);
            }
        }
    }


    private static Object handleAllEmployeePayrollDetailed(Request req, Response res) throws SQLException {
        String token = req.queryParams("token");
        String adminIdStr = req.queryParams("adminId");
    
        // Validate required parameters
        if (token == null || adminIdStr == null) {
            res.status(400);
            return gson.toJson(Map.of("error", "Missing adminId or token"));
        }
    
        int adminId;
        try {
            adminId = Integer.parseInt(adminIdStr);
        } catch (NumberFormatException e) {
            res.status(400);
            return gson.toJson(Map.of("error", "Invalid adminId"));
        }
    
        // Validate admin session
        if (!validateSession(adminId, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }
    
        try (Connection conn = getConnection()) {
            String sql = """
                SELECT 
                    e.empid,
                    e.first_name,
                    e.last_name,
                    e.job_title,
                    e.hourly_rate,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600, 2) AS total_hours,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate, 2) AS gross_pay,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 0.20, 2) AS tax,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 0.05, 2) AS retirement,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 0.75, 2) AS net_pay,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 4.33, 2) AS monthly_gross,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 4.33 * 0.20, 2) AS monthly_tax,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 4.33 * 0.05, 2) AS monthly_retirement,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, t.punch_in, t.punch_out)/3600) * e.hourly_rate * 4.33 * 0.75, 2) AS monthly_net
                FROM employees e
                LEFT JOIN time_logs t ON e.empid = t.empid AND t.punch_out IS NOT NULL
                WHERE e.is_active = TRUE
                GROUP BY e.empid, e.first_name, e.last_name, e.job_title, e.hourly_rate
                ORDER BY e.empid
            """;
    
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
    
                List<Map<String, Object>> payrollData = new ArrayList<>();
    
                while (rs.next()) {
                    Map<String, Object> employee = new HashMap<>();
                    // Basic employee info
                    employee.put("empid", rs.getInt("empid"));
                    employee.put("first_name", rs.getString("first_name"));
                    employee.put("last_name", rs.getString("last_name"));
                    employee.put("full_name", rs.getString("first_name") + " " + rs.getString("last_name"));
                    employee.put("job_title", rs.getString("job_title"));
                    employee.put("hourly_rate", rs.getDouble("hourly_rate"));
                    
                    // Hours worked
                    employee.put("hours_worked", rs.getDouble("total_hours"));
                    
                    // Weekly breakdown
                    employee.put("weekly_gross", rs.getDouble("gross_pay"));
                    employee.put("weekly_tax", rs.getDouble("tax"));
                    employee.put("weekly_retirement", rs.getDouble("retirement"));
                    employee.put("weekly_net", rs.getDouble("net_pay"));
                    
                    // Monthly breakdown
                    employee.put("monthly_gross", rs.getDouble("monthly_gross"));
                    employee.put("monthly_tax", rs.getDouble("monthly_tax"));
                    employee.put("monthly_retirement", rs.getDouble("monthly_retirement"));
                    employee.put("monthly_net", rs.getDouble("monthly_net"));
                    
                    payrollData.add(employee);
                }
    
                return gson.toJson(payrollData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            res.status(500);
            return gson.toJson(Map.of("error", "Database error: " + e.getMessage()));
        }
    }
    private static Object handleGetEmployees(Request req, Response res) throws SQLException {
        String search = req.queryParams("search");

        String sql = "SELECT e.empid, e.first_name, e.last_name, e.job_title, e.hourly_rate, " +
                     "ROUND(SUM(CASE WHEN punch_in >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN TIMESTAMPDIFF(SECOND, punch_in, punch_out) ELSE 0 END)/3600, 2) as hours_week, " +
                     "ROUND(SUM(CASE WHEN punch_in >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN TIMESTAMPDIFF(SECOND, punch_in, punch_out) ELSE 0 END)/3600, 2) as hours_month " +
                     "FROM employees e LEFT JOIN time_logs t ON e.empid = t.empid AND t.punch_out IS NOT NULL " +
                     "WHERE e.is_active = TRUE " +
                     (search != null && !search.isEmpty() ? 
                      "AND (e.first_name LIKE ? OR e.last_name LIKE ? OR e.empid = ?) " : "") +
                     "GROUP BY e.empid, e.first_name, e.last_name, e.job_title, e.hourly_rate";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (search != null && !search.isEmpty()) {
                stmt.setString(1, "%" + search + "%");
                stmt.setString(2, "%" + search + "%");
                try {
                    stmt.setInt(3, Integer.parseInt(search));
                } catch (NumberFormatException e) {
                    stmt.setInt(3, -1);
                }
            }

            ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> employees = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> emp = new HashMap<>();
                emp.put("empid", rs.getInt("empid"));
                emp.put("first_name", rs.getString("first_name"));
                emp.put("last_name", rs.getString("last_name"));
                emp.put("job_title", rs.getString("job_title"));
                double rate = rs.getDouble("hourly_rate");
                double hoursWeek = rs.getDouble("hours_week");
                double hoursMonth = rs.getDouble("hours_month");

                emp.put("hourly_rate", rate);
                emp.put("hours_worked", hoursWeek);
                emp.put("weekly_pay", Math.round(hoursWeek * rate * 100.0) / 100.0);
                emp.put("monthly_pay", Math.round(hoursMonth * rate * 100.0) / 100.0);

                employees.add(emp);
            }
            return gson.toJson(employees);
        }
    }

    private static Object handleCreateEmployee(Request req, Response res) throws SQLException {
        Map<String, Object> request = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        String token = (String) request.get("token");
        int adminId = ((Double) request.get("adminId")).intValue();
    
        if (!validateSession(adminId, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }
    
        // Safely parse all fields
        String firstName = (String) request.get("firstName");
        String lastName = (String) request.get("lastName");
        String email = (String) request.get("email");
        String jobTitle = (String) request.get("jobTitle");
        
        // Improved hourly rate handling
        Double hourlyRate;
        try {
            Object rateObj = request.get("hourlyRate");
            if (rateObj instanceof Double) {
                hourlyRate = (Double) rateObj;
            } else if (rateObj instanceof String) {
                hourlyRate = Double.parseDouble((String) rateObj);
            } else {
                throw new IllegalArgumentException("Invalid hourly rate format");
            }
        } catch (Exception e) {
            res.status(400);
            return gson.toJson(Map.of("error", "Invalid hourly rate: " + e.getMessage()));
        }
    
        // Validate required fields
        if (firstName == null || lastName == null || email == null || 
            hourlyRate == null || jobTitle == null) {
            res.status(400);
            return gson.toJson(Map.of("error", "Missing required fields"));
        }
    
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO employees (first_name, last_name, email, hourly_rate, job_title) " +
                         "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, email);
                stmt.setDouble(4, hourlyRate);
                stmt.setString(5, jobTitle);
                stmt.executeUpdate();
    
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int empid = rs.getInt(1);
                    return gson.toJson(Map.of("success", true, "empid", empid));
                }
                return gson.toJson(Map.of("success", false));
            }
        }
    }





    private static Object handleLogout(Request req, Response res) throws SQLException {
        Map<String, Object> request = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        int empid = ((Double) request.get("empid")).intValue();
        String token = (String) request.get("token");

        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM user_sessions WHERE empid = ? AND token = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                stmt.setString(2, token);
                int deleted = stmt.executeUpdate();
                return gson.toJson(Map.of("success", deleted > 0));
            }
        }
    }

    private static Object handleUpdatePay(Request req, Response res) throws SQLException {
        Map<String, Object> body = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        int empid = ((Double) body.get("empid")).intValue();
        double newRate = ((Double) body.get("hourlyRate")).doubleValue();

        try (Connection conn = getConnection()) {
            String sql = "UPDATE employees SET hourly_rate = ? WHERE empid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, newRate);
                stmt.setInt(2, empid);
                int updated = stmt.executeUpdate();
                return gson.toJson(Map.of("success", updated > 0));
            }
        }
    }

    private static Object handleUpdateTitle(Request req, Response res) throws SQLException {
        Map<String, Object> body = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        int empid = ((Double) body.get("empid")).intValue();
        String newTitle = (String) body.get("jobTitle");

        try (Connection conn = getConnection()) {
            String sql = "UPDATE employees SET job_title = ? WHERE empid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newTitle);
                stmt.setInt(2, empid);
                int updated = stmt.executeUpdate();
                return gson.toJson(Map.of("success", updated > 0));
            }
        }
    }

    private static Object handleDeactivateEmployee(Request req, Response res) throws SQLException {
        Map<String, Object> body = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        int empid = ((Double) body.get("empid")).intValue();
        String token = (String) body.get("token");
        int adminId = ((Double) body.get("adminId")).intValue();

        if (!validateSession(adminId, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }

        try (Connection conn = getConnection()) {
            String sql = "UPDATE employees SET is_active = FALSE WHERE empid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                int updated = stmt.executeUpdate();
                return gson.toJson(Map.of("success", updated > 0));
            }
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            dotenv.get("DB_URL"),
            dotenv.get("DB_USER"),
            dotenv.get("DB_PASSWORD")
        );
    }

    private static void storeSession(Connection conn, int empid, String token) throws SQLException {
        String sql = "INSERT INTO user_sessions (empid, token, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 8 HOUR))";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empid);
            stmt.setString(2, token);
            stmt.executeUpdate();
        }
    }

    private static boolean validateSession(int empid, String token) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT COUNT(*) FROM user_sessions WHERE empid = ? AND token = ? AND expires_at > NOW()";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                stmt.setString(2, token);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean isClockedIn(Connection conn, int empid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM time_logs WHERE empid = ? AND punch_out IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empid);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
