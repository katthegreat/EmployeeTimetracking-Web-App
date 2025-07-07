package TimeTrackingBackend;

import java.sql.*;

public class ProfileViewer {
    public static void showProfile(Connection conn, int empId) throws SQLException {
        String query = "SELECT * FROM employees WHERE empid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, empId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n👤 Your Profile:");
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.println("Job Title: " + rs.getString("job_title"));
                    System.out.printf("Hourly Rate: $%.2f%n", rs.getDouble("hourly_rate"));
                } else {
                    System.out.println("⚠️ Employee profile not found.");
                }
            }
        }
    }
}
