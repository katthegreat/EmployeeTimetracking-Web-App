package TimeTrackingBackend;

import java.sql.*;
import java.util.Scanner;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Load .env from lib/
        Dotenv dotenv = Dotenv.configure()
                              .directory("lib")
                              .load();

        String dbUrl = dotenv.get("DB_URL");
        String dbUser = dotenv.get("DB_USER");
        String dbPassword = dotenv.get("DB_PASSWORD");

        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            // Ensure default admin exists
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            Statement adminCheckStmt = conn.createStatement();
            ResultSet adminRs = adminCheckStmt.executeQuery(checkAdmin);
            adminRs.next();
            if (adminRs.getInt(1) == 0) {
                String insertAdmin = """
                    INSERT INTO users (username, password, role, empid)
                    VALUES ('admin', 'adminpass', 'admin', 1)
                """;
                try {
                    int rows = adminCheckStmt.executeUpdate(insertAdmin);
                    if (rows > 0) {
                        System.out.println("✅ Default admin account created.");
                    }
                } catch (SQLException e) {
                    System.out.println("⚠️ Could not create default admin. Make sure empid=1 exists in employees.");
                }
            }
            adminRs.close();
            adminCheckStmt.close();

            // Attempt login
            String loginQuery = "SELECT role, empid FROM users WHERE username = ? AND password = ?";
            PreparedStatement loginStmt = conn.prepareStatement(loginQuery);
            loginStmt.setString(1, username);
            loginStmt.setString(2, password);
            ResultSet rs = loginStmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                Integer empId = rs.getInt("empid");
                System.out.println("✅ Login successful as " + role);

                if ("admin".equals(role)) {
                    AdminMenu.run(conn, scanner);
                } else {
                    EmployeeMenu.run(conn, scanner, empId);
                }
            } else {
                System.out.println("❌ Invalid credentials.");
            }

            rs.close();
            loginStmt.close();
            conn.close();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
