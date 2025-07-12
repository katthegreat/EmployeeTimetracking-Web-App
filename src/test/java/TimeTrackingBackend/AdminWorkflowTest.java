package TimeTrackingBackend;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;

public class AdminWorkflowTest {
    private static Connection conn;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/timetracking";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "YourNewPassword123!";

    @BeforeAll
    static void setup() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Scenario 1: Admin views active employees
    @Test
    @Order(1)
    void testAdminViewsActiveEmployees() throws SQLException {
        System.out.println("\nSCENARIO 1: Admin views active employees");
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT first_name, last_name, job_title FROM employees WHERE is_active=1")) {
            
            System.out.println("Active Employees:");
            boolean hasEmployees = false;
            while (rs.next()) {
                hasEmployees = true;
                System.out.printf("- %s %s (%s)%n",
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("job_title"));
            }
            assertTrue(hasEmployees, "No active employees found");
        }
        System.out.println("✅ Scenario 1 Passed");
    }

    // Scenario 2: Admin updates employee info
    @Test
    @Order(2)
    void testAdminUpdatesEmployee() throws SQLException {
        System.out.println("\nSCENARIO 2: Admin updates employee info");
        int testEmployeeId = 1;
        
        // Get original data
        String originalTitle;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT job_title FROM employees WHERE employee_id=" + testEmployeeId)) {
            assertTrue(rs.next(), "Employee not found");
            originalTitle = rs.getString("job_title");
            System.out.printf("Original: %s%n", originalTitle);
        }

        // Update employee
        String newTitle = "Updated " + originalTitle;
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE employees SET job_title=? WHERE employee_id=?")) {
            stmt.setString(1, newTitle);
            stmt.setInt(2, testEmployeeId);
            assertEquals(1, stmt.executeUpdate(), "Update failed");
        }

        // Verify changes
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT job_title FROM employees WHERE employee_id=" + testEmployeeId)) {
            assertTrue(rs.next(), "Employee missing after update");
            assertEquals(newTitle, rs.getString("job_title"), "Title not updated");
            System.out.printf("Updated: %s%n", newTitle);
        }
        System.out.println("✅ Scenario 2 Passed");
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (conn != null) conn.close();
    }
}
