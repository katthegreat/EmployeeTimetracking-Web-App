package TimeTrackingBackend;

import static spark.Spark.*;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.Properties;

public class LoginServer {
    public static void main(String[] args) {
        port(4567);

        // Allow CORS
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        // LOGIN route
        post("/api/login", (request, response) -> {
            response.type("application/json");

            JSONObject reqJson = new JSONObject(request.body());
            String username = reqJson.getString("username");
            String password = reqJson.getString("password");

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                Dotenv dotenv = Dotenv.load();
                String dbUrl = dotenv.get("DB_URL");
                String dbUser = dotenv.get("DB_USER");
                String dbPassword = dotenv.get("DB_PASSWORD");

                Properties props = new Properties();
                props.setProperty("user", dbUser);
                props.setProperty("password", dbPassword);
                props.setProperty("allowPublicKeyRetrieval", "true");
                props.setProperty("useSSL", "false");

                conn = DriverManager.getConnection(dbUrl, props);

                String sql = "SELECT u.role, e.name FROM users u LEFT JOIN employees e ON u.empid = e.empid WHERE u.username=? AND u.password=?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                rs = stmt.executeQuery();

                JSONObject resJson = new JSONObject();
                if (rs.next()) {
                    String role = rs.getString("role");
                    String name = rs.getString("name");
                    resJson.put("success", true);
                    resJson.put("role", role);
                    resJson.put("employeeName", name != null ? name : "Admin");
                } else {
                    resJson.put("success", false);
                }

                return resJson.toString();

            } catch (Exception e) {
                e.printStackTrace();
                JSONObject resJson = new JSONObject();
                resJson.put("success", false);
                resJson.put("error", "Server error");
                return resJson.toString();
            } finally {
                try { if (rs != null) rs.close(); } catch (Exception e) {}
                try { if (stmt != null) stmt.close(); } catch (Exception e) {}
                try { if (conn != null) conn.close(); } catch (Exception e) {}
            }
        });

        // GET all employees
        get("/api/employees", (req, res) -> {
            Dotenv dotenv = Dotenv.load();
            String dbUrl = dotenv.get("DB_URL");
            String dbUser = dotenv.get("DB_USER");
            String dbPassword = dotenv.get("DB_PASSWORD");

            Properties props = new Properties();
            props.setProperty("user", dbUser);
            props.setProperty("password", dbPassword);
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("useSSL", "false");

            Connection conn = DriverManager.getConnection(dbUrl, props);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM employees");

            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                json.append(String.format(
                    "{\"empid\":%d,\"name\":\"%s\",\"position\":\"%s\",\"salary\":%.2f},",
                    rs.getInt("empid"),
                    rs.getString("name"),
                    rs.getString("job_title"),
                    rs.getDouble("hourly_rate")
                ));
            }

            if (json.charAt(json.length() - 1) == ',') {
                json.setLength(json.length() - 1);
            }
            json.append("]");

            rs.close();
            stmt.close();
            conn.close();

            res.type("application/json");
            return json.toString();
        });

        // CLOCK IN
        post("/api/clockin", (request, response) -> {
            JSONObject reqJson = new JSONObject(request.body());
            String name = reqJson.getString("username");

            Dotenv dotenv = Dotenv.load();
            Connection conn = DriverManager.getConnection(dotenv.get("DB_URL"), dotenv.get("DB_USER"), dotenv.get("DB_PASSWORD"));

            PreparedStatement stmt = conn.prepareStatement("INSERT INTO attendance (name, action, timestamp) VALUES (?, 'IN', NOW())");
            stmt.setString(1, name);
            stmt.executeUpdate();

            stmt.close();
            conn.close();

            response.type("application/json");
            return new JSONObject().put("message", "Clocked in!").toString();
        });

        // CLOCK OUT
        post("/api/clockout", (request, response) -> {
            JSONObject reqJson = new JSONObject(request.body());
            String name = reqJson.getString("username");

            Dotenv dotenv = Dotenv.load();
            Connection conn = DriverManager.getConnection(dotenv.get("DB_URL"), dotenv.get("DB_USER"), dotenv.get("DB_PASSWORD"));

            PreparedStatement stmt = conn.prepareStatement("INSERT INTO attendance (name, action, timestamp) VALUES (?, 'OUT', NOW())");
            stmt.setString(1, name);
            stmt.executeUpdate();

            stmt.close();
            conn.close();

            response.type("application/json");
            return new JSONObject().put("message", "Clocked out!").toString();
        });

        // VIEW PAYROLL
        get("/api/payroll", (request, response) -> {
            String name = request.queryParams("username");

            Dotenv dotenv = Dotenv.load();
            Connection conn = DriverManager.getConnection(dotenv.get("DB_URL"), dotenv.get("DB_USER"), dotenv.get("DB_PASSWORD"));

            PreparedStatement hoursStmt = conn.prepareStatement(
                "SELECT COUNT(*) / 2.0 AS hours_worked FROM attendance WHERE name = ?"
            );
            hoursStmt.setString(1, name);
            ResultSet rs = hoursStmt.executeQuery();

            double hoursWorked = 0.0;
            if (rs.next()) {
                hoursWorked = rs.getDouble("hours_worked");
            }

            PreparedStatement rateStmt = conn.prepareStatement("SELECT hourly_rate FROM employees WHERE name = ?");
            rateStmt.setString(1, name);
            ResultSet rateRs = rateStmt.executeQuery();

            double hourlyRate = 0.0;
            if (rateRs.next()) {
                hourlyRate = rateRs.getDouble("hourly_rate");
            }

            rs.close();
            rateRs.close();
            hoursStmt.close();
            rateStmt.close();
            conn.close();

            double totalPay = hoursWorked * hourlyRate;

            JSONObject resJson = new JSONObject();
            resJson.put("hoursWorked", hoursWorked);
            resJson.put("hourlyRate", hourlyRate);
            resJson.put("totalPay", totalPay);

            response.type("application/json");
            return resJson.toString();
        });
    }
}
