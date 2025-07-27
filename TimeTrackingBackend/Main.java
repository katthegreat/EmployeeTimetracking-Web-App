package TimeTrackingBackend;

import java.sql.*;
import java.util.Scanner;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    private static final int MAX_ATTEMPTS = 3;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection conn = null;

        try {
            // Load environment variables
            Dotenv dotenv = Dotenv.configure()
                                .directory("lib")
                                .ignoreIfMissing()
                                .load();

            // Connect to database
            System.out.println("🔌 Connecting to database...");
            conn = DriverManager.getConnection(
                dotenv.get("DB_URL"),
                dotenv.get("DB_USER"),
                dotenv.get("DB_PASSWORD")
            );
            System.out.println("✅ Connected to database");

            int attempts = 0;
            boolean authenticated = false;

            while (attempts < MAX_ATTEMPTS && !authenticated) {
                System.out.println("\n=== LOGIN ===");
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Password: ");
                String password = scanner.nextLine();

                String sql = "SELECT role, empid FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        authenticated = true;
                        String role = rs.getString("role");
                        int empid = rs.getInt("empid");

                        System.out.printf("\n✅ Login successful! Role: %s\n", role);

                        if (role.equals("admin")) {
                            AdminMenu.run(conn, scanner);
                        } else {
                            EmployeeMenu.run(conn, scanner, empid);
                        }
                    } else {
                        System.out.println("❌ Invalid credentials. Try again.");
                        attempts++;
                    }
                }
            }

            if (!authenticated) {
                System.out.println("❌ Too many failed attempts. Exiting.");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
                scanner.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
            System.out.println("👋 Goodbye!");
        }
    }
}
