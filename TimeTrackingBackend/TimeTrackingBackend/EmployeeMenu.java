package TimeTrackingBackend;

import java.sql.*;
import java.util.Scanner;

public class EmployeeMenu {

    public static void run(Connection conn, Scanner scanner, int empId) throws SQLException {
        while (true) {
            System.out.println("\nEmployee Menu:");
            System.out.println("1. Punch In");
            System.out.println("2. Punch Out");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> punchIn(conn, empId);
                case "2" -> punchOut(conn, empId);
                case "3" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void punchIn(Connection conn, int empId) throws SQLException {
        String check = "SELECT COUNT(*) FROM time_logs WHERE employee_id = ? AND punch_out IS NULL";
        try (PreparedStatement checkStmt = conn.prepareStatement(check)) {
            checkStmt.setInt(1, empId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("⚠️ Already punched in.");
                    return;
                }
            }
        }

        String insert = "INSERT INTO time_logs (employee_id, punch_in) VALUES (?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(insert)) {
            stmt.setInt(1, empId);
            stmt.executeUpdate();
            System.out.println("✅ Punch in recorded.");
        }
    }

    private static void punchOut(Connection conn, int empId) throws SQLException {
        String update = "UPDATE time_logs SET punch_out = NOW() WHERE employee_id = ? AND punch_out IS NULL ORDER BY punch_in DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(update)) {
            stmt.setInt(1, empId);
            int rows = stmt.executeUpdate();

            if (rows > 0) System.out.println("✅ Punch out recorded.");
            else System.out.println("⚠️ No active punch-in found.");
        }
    }
}