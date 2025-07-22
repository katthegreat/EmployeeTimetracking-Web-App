package TimeTrackingBackend;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import spark.Request;
import spark.Response;
import java.sql.*;
import java.util.*;
import io.github.cdimascio.dotenv.Dotenv;

public class LoginServer {
    private static final Gson gson = new Gson();
    private static Dotenv dotenv;

    public static void main(String[] args) {
        // Configure server
        port(4567);
        
        // Load environment variables
        dotenv = Dotenv.configure()
                .directory("lib")
                .ignoreIfMissing()
                .load();

        // Enable CORS
        enableCORS();

        // Setup routes
        setupRoutes();

        // Initialize database connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void enableCORS() {
        options("/*", (req, res) -> {
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
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
        // Login endpoint
        post("/api/login", "application/json", LoginServer::handleLogin);
        
        // Time clock endpoints
        post("/api/clock-in", "application/json", LoginServer::handleClockIn);
        post("/api/clock-out", "application/json", LoginServer::handleClockOut);
        
        // Employee endpoints
        get("/api/employees", LoginServer::handleGetEmployees);
        get("/api/clock-status", LoginServer::handleClockStatus);
        get("/api/weekly-summary", LoginServer::handleWeeklySummary);
        post("/api/employees", "application/json", LoginServer::handleCreateEmployee);
        post("/api/logout", "application/json", LoginServer::handleLogout);
        
        // Error handling
        exception(Exception.class, (e, req, res) -> {
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

    private static Object handleClockIn(Request req, Response res) throws SQLException {
        Map<String, Object> request = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        int empid = ((Double) request.get("empid")).intValue();
        String token = (String) request.get("token");
        
        if (!validateSession(empid, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }

        try (Connection conn = getConnection()) {
            if (isClockedIn(conn, empid)) {
                res.status(400);
                return gson.toJson(Map.of("error", "Already clocked in"));
            }
            
            String sql = "INSERT INTO time_logs (empid, punch_in, notes) VALUES (?, NOW(), ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                stmt.setString(2, (String) request.getOrDefault("notes", ""));
                stmt.executeUpdate();
                
                return gson.toJson(Map.of("success", true, "action", "clock-in"));
            }
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

        try (Connection conn = getConnection()) {
            String sql = "UPDATE time_logs SET punch_out = NOW() " +
                        "WHERE empid = ? AND punch_out IS NULL " +
                        "ORDER BY punch_in DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    res.status(400);
                    return gson.toJson(Map.of("error", "No active clock-in found"));
                }
                return gson.toJson(Map.of("success", true, "action", "clock-out"));
            }
        }
    }

    private static Object handleGetEmployees(Request req, Response res) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM employees WHERE is_active = TRUE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                List<Map<String, Object>> employees = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> emp = new HashMap<>();
                    emp.put("empid", rs.getInt("empid"));
                    emp.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                    emp.put("job_title", rs.getString("job_title"));
                    employees.add(emp);
                }
                return gson.toJson(employees);
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

    private static Object handleCreateEmployee(Request req, Response res) throws SQLException {
        Map<String, Object> request = gson.fromJson(req.body(), new TypeToken<Map<String, Object>>(){}.getType());
        String token = (String) request.get("token");
        int adminId = ((Double) request.get("adminId")).intValue();
        
        if (!validateSession(adminId, token)) {
            res.status(401);
            return gson.toJson(Map.of("error", "Invalid session"));
        }

        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO employees (first_name, last_name, email, hourly_rate, job_title) " +
                        "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, (String) request.get("firstName"));
                stmt.setString(2, (String) request.get("lastName"));
                stmt.setString(3, (String) request.get("email"));
                stmt.setDouble(4, ((Double) request.get("hourlyRate")).doubleValue());
                stmt.setString(5, (String) request.get("jobTitle"));
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

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            dotenv.get("DB_URL"),
            dotenv.get("DB_USER"),
            dotenv.get("DB_PASSWORD")
        );
    }

    private static void storeSession(Connection conn, int empid, String token) throws SQLException {
        String sql = "INSERT INTO user_sessions (empid, token, expires_at) " +
                     "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 8 HOUR))";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empid);
            stmt.setString(2, token);
            stmt.executeUpdate();
        }
    }

    private static boolean validateSession(int empid, String token) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT COUNT(*) FROM user_sessions " +
                         "WHERE empid = ? AND token = ? AND expires_at > NOW()";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, empid);
                stmt.setString(2, token);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean isClockedIn(Connection conn, int empid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM time_logs " +
                     "WHERE empid = ? AND punch_out IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empid);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}