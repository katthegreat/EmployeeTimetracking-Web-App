package EmployeeTimeTracking.TimeTrackingBackend;

import java.sql.*;
import java.util.Scanner;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Load .env
        Dotenv dotenv = Dotenv.configure()
                              .directory("lib")
                              .load();

        String dbUrl = dotenv.get("DB_URL");
        String dbUser = dotenv.get("DB_USER");
        String dbPassword = dotenv.get("DB_PASSWORD");

        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            // Ensure default admin exists
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            try (Statement adminCheckStmt = conn.createStatement();
                 ResultSet adminRs = adminCheckStmt.executeQuery(checkAdmin)) {
                adminRs.next();
                if (adminRs.getInt(1) == 0) {
                    String insertAdmin = """
                        INSERT INTO users (username, password, role, employee_id)
                        VALUES ('admin', 'adminpass', 'admin', 1)
                    """;
                    adminCheckStmt.executeUpdate(insertAdmin);
                    System.out.println("✅ Default admin account created.");
                }
            }

            boolean loggedIn = false;

            // If admin login
            if (username.equals("admin")) {
                String loginQuery = "SELECT role, employee_id FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement loginStmt = conn.prepareStatement(loginQuery)) {
                    loginStmt.setString(1, username);
                    loginStmt.setString(2, password);
                    try (ResultSet rs = loginStmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("✅ Login successful as admin.");
                            AdminMenu.run(conn, scanner);
                            loggedIn = true;
                        }
                    }
                }
            } else {
                // Otherwise, check employees with default 'emppass'
                if (password.equals("emppass")) {
                    String empQuery = "SELECT employee_id FROM employees WHERE first_name = ?";
                    try (PreparedStatement empStmt = conn.prepareStatement(empQuery)) {
                        empStmt.setString(1, username);
                        try (ResultSet rs = empStmt.executeQuery()) {
                            if (rs.next()) {
                                int empId = rs.getInt("employee_id");
                                System.out.println("✅ Login successful as employee.");
                                EmployeeMenu.run(conn, scanner, empId);
                                loggedIn = true;
                            }
                        }
                    }
                }
            }

            if (!loggedIn) {
                System.out.println("❌ Invalid credentials.");
            }

            conn.close();
            scanner.close();
        } catch (Exception e) {
            System.out.println("⚠️ An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
