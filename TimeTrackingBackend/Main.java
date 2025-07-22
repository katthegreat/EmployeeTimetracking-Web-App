package TimeTrackingBackend;

import java.sql.*;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection conn = null;

        try {
            // Load environment variables
            Dotenv dotenv = Dotenv.configure()
                                .directory("lib")
                                .ignoreIfMissing()
                                .load();

            // Initialize database connection
            conn = DriverManager.getConnection(
                dotenv.get("DB_URL"),
                dotenv.get("DB_USER"),
                dotenv.get("DB_PASSWORD")
            );
            System.out.println("✅ Database connection established");

            // Login process
            boolean authenticated = false;
            int attempts = 0;

            while (!authenticated && attempts < MAX_LOGIN_ATTEMPTS) {
                System.out.println("\n=== TIME TRACKING SYSTEM ===");
                System.out.printf("Login Attempt %d/%d%n", attempts + 1, MAX_LOGIN_ATTEMPTS);
                
                // Get credentials
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Password: ");
                String password = scanner.nextLine();

                // Plain text authentication
                String sql = "SELECT u.role, u.empid, e.first_name, e.last_name " +
                            "FROM users u JOIN employees e ON u.empid = e.empid " +
                            "WHERE u.username = ? AND u.password = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            authenticated = true;
                            String role = rs.getString("role");
                            int empId = rs.getInt("empid");
                            String firstName = rs.getString("first_name");
                            String lastName = rs.getString("last_name");
                            
                            System.out.printf("\n✅ Welcome, %s %s!%n", firstName, lastName);
                            
                            if (role.equals("admin")) {
                                AdminMenu.run(conn, scanner);
                            } else {
                                EmployeeMenu.run(conn, scanner, empId);
                            }
                        } else {
                            attempts++;
                            System.out.println("\n❌ Invalid credentials. Please try again.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("\n⚠️ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
                scanner.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
            System.out.println("\nThank you for using Time Tracking System. Goodbye!");
        }
    }
}