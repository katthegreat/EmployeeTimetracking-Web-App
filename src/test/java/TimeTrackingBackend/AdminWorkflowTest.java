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
        System.out.println("Database connection established");
    }

    // Scenario 1: Admin views active employees
    @Test
    @Order(1)
    @DisplayName("Admin views active employees")
    void testAdminViewsActiveEmployees() throws SQLException {
        System.out.println("\n--- Testing Admin Views Active Employees ---");
        
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
        System.out.println("✅ Scenario 1 Passed: Admin successfully viewed employees\n");
    }

    // Scenario 2: Admin updates employee information
    @Test
    @Order(2)
    @DisplayName("Admin updates employee information")
    void testAdminUpdatesEmployee() throws SQLException {
        System.out.println("--- Testing Admin Updates Employee ---");
        int testEmployeeId = 1; // Test with employee ID 1
        
        // 1. Get original data
        String originalTitle;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT job_title FROM employees WHERE employee_id=" + testEmployeeId)) {
            assertTrue(rs.next(), "Employee not found");
            originalTitle = rs.getString("job_title");
            System.out.printf("Original Job Title: %s%n", originalTitle);
        }

        // 2. Update employee
        String newTitle = "Updated " + originalTitle;
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE employees SET job_title=? WHERE employee_id=?")) {
            stmt.setString(1, newTitle);
            stmt.setInt(2, testEmployeeId);
            assertEquals(1, stmt.executeUpdate(), "Update failed");
        }

        // 3. Verify changes
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT job_title FROM employees WHERE employee_id=" + testEmployeeId)) {
            assertTrue(rs.next(), "Employee missing after update");
            assertEquals(newTitle, rs.getString("job_title"), "Title not updated");
            System.out.printf("Updated Job Title: %s%n", newTitle);
        }
        System.out.println("✅ Scenario 2 Passed: Admin successfully updated employee\n");
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (conn != null) {
            conn.close();
            System.out.println("Database connection closed");
        }
    }
}
