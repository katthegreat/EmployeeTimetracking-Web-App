package TimeTrackingBackend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.InputMismatchException;

public class AdminMenu {

    public static void run(Connection conn, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View all employees");
            System.out.println("2. Update employee hourly rate");
            System.out.println("3. Update employee job title");
            System.out.println("4. Search employee by ID");
            System.out.println("5. Exit");
            System.out.print("Choose an option (1-5): ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        viewAllEmployees(conn);
                        break;
                    case "2":
                        updateHourlyRate(conn, scanner);
                        break;
                    case "3":
                        updateJobTitle(conn, scanner);
                        break;
                    case "4":
                        searchEmployeeById(conn, scanner);
                        break;
                    case "5":
                        System.out.println("Returning to main menu...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter 1-5.");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format. Please try again.");
            }
        }
    }

    private static void viewAllEmployees(Connection conn) throws SQLException {
        String query = "SELECT empid, first_name, last_name, hourly_rate, job_title " +
                      "FROM employees ORDER BY last_name, first_name";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (!rs.isBeforeFirst()) {
                System.out.println("\nNo employees found in the database.");
                return;
            }

            System.out.println("\n=== EMPLOYEE LIST ===");
            System.out.printf("%-6s %-20s %-10s %-20s%n", 
                            "ID", "Name", "Rate", "Job Title");
            System.out.println("------------------------------------------------");
            
            while (rs.next()) {
                System.out.printf("%-6d %-20s $%-9.2f %-20s%n",
                    rs.getInt("empid"),
                    rs.getString("last_name") + ", " + rs.getString("first_name"),
                    rs.getDouble("hourly_rate"),
                    rs.getString("job_title"));
            }
        }
    }

    private static void updateHourlyRate(Connection conn, Scanner scanner) 
            throws SQLException, NumberFormatException {
        
        int empid = getValidEmployeeId(scanner);
        System.out.print("Enter new hourly rate: ");
        double rate = Double.parseDouble(scanner.nextLine());

        if (rate <= 0) {
            System.out.println("Hourly rate must be positive.");
            return;
        }

        String sql = "UPDATE employees SET hourly_rate = ? WHERE empid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, rate);
            stmt.setInt(2, empid);
            int rows = stmt.executeUpdate();

            System.out.println(rows > 0 ? "✅ Updated hourly rate." : "❌ Employee not found.");
        }
    }

    private static void updateJobTitle(Connection conn, Scanner scanner) 
            throws SQLException, NumberFormatException {
        
        int empid = getValidEmployeeId(scanner);
        System.out.print("Enter new job title: ");
        String title = scanner.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("Job title cannot be empty.");
            return;
        }

        String sql = "UPDATE employees SET job_title = ? WHERE empid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setInt(2, empid);
            int rows = stmt.executeUpdate();

            System.out.println(rows > 0 ? "✅ Updated job title." : "❌ Employee not found.");
        }
    }

    private static void searchEmployeeById(Connection conn, Scanner scanner) 
            throws SQLException, NumberFormatException {
        
        int empid = getValidEmployeeId(scanner);
        String sql = "SELECT empid, first_name, last_name, hourly_rate, job_title " +
                    "FROM employees WHERE empid = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n=== EMPLOYEE DETAILS ===");
                System.out.printf("%-15s: %d%n", "Employee ID", rs.getInt("empid"));
                System.out.printf("%-15s: %s %s%n", "Name", 
                                rs.getString("first_name"), rs.getString("last_name"));
                System.out.printf("%-15s: $%.2f%n", "Hourly Rate", rs.getDouble("hourly_rate"));
                System.out.printf("%-15s: %s%n", "Job Title", rs.getString("job_title"));
            } else {
                System.out.println("❌ Employee not found.");
            }
        }
    }

    private static int getValidEmployeeId(Scanner scanner) throws NumberFormatException {
        while (true) {
            try {
                System.out.print("Enter employee ID: ");
                int id = Integer.parseInt(scanner.nextLine());
                if (id > 0) return id;
                System.out.println("ID must be positive. Try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter digits only.");
            }
        }
    }
}