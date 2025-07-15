package TimeTrackingBackend;

import java.sql.*;
import java.util.Scanner;

public class AdminMenu {

    public static void run(Connection conn, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\nAdmin Menu:");
            System.out.println("1. View all employees");
            System.out.println("2. Update employee hourly rate");
            System.out.println("3. Update employee job title");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> viewAllEmployees(conn);
                case "2" -> updateHourlyRate(conn, scanner);
                case "3" -> updateJobTitle(conn, scanner);
                case "4" -> {
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void viewAllEmployees(Connection conn) throws SQLException {
        String query = "SELECT * FROM employees ORDER BY employee_id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("\nEmployee List:");
            while (rs.next()) {
                System.out.printf("%d | %s %s | $%.2f | %s%n",
                        rs.getInt("employee_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getDouble("hourly_rate"),
                        rs.getString("job_title"));
            }
        }
    }

    private static void updateHourlyRate(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter employee ID: ");
        int employeeId = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter new hourly rate: ");
        double rate = Double.parseDouble(scanner.nextLine());

        String sql = "UPDATE employees SET hourly_rate = ? WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, rate);
            stmt.setInt(2, employeeId);
            int rows = stmt.executeUpdate();

            if (rows > 0) System.out.println("✅ Updated hourly rate.");
            else System.out.println("❌ Employee not found.");
        }
    }

    private static void updateJobTitle(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter employee ID: ");
        int employeeId = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter new job title: ");
        String title = scanner.nextLine();

        String sql = "UPDATE employees SET job_title = ? WHERE employee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setInt(2, employeeId);
            int rows = stmt.executeUpdate();

            if (rows > 0) System.out.println("✅ Updated job title.");
            else System.out.println("❌ Employee not found.");
        }
    }
}