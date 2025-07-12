package tests;

import org.junit.jupiter.api.*;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class AdminMenuTest {

    private static Connection conn;

    @BeforeAll
    public static void setupDatabase() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/timetracking", "root", "Mitakedame12"
        );
    }

    @AfterAll
    public static void closeConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    public void testViewAllEmployees() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM employees");

            assertNotNull(rs);
            boolean hasData = rs.next();
            assertTrue(hasData, "Expected at least one employee in the table");

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            fail("SQL error: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateHourlyRate() {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE employees SET hourly_rate = 22.5 WHERE empid = 1");

            ResultSet rs = stmt.executeQuery("SELECT hourly_rate FROM employees WHERE empid = 1");
            if (rs.next()) {
                double rate = rs.getDouble("hourly_rate");
                assertEquals(22.5, rate, 0.01, "Hourly rate should be updated to 22.5");
            } else {
                fail("Employee with empid = 1 not found");
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            fail("SQL error: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateJobTitle() {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE employees SET job_title = 'Developer' WHERE empid = 1");

            ResultSet rs = stmt.executeQuery("SELECT job_title FROM employees WHERE empid = 1");
            if (rs.next()) {
                String title = rs.getString("job_title");
                assertEquals("Developer", title, "Job title should be updated to 'Developer'");
            } else {
                fail("Employee with empid = 1 not found");
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            fail("SQL error: " + e.getMessage());
        }
    }

    @Test
public void testInsertAndRetrieveEmployee() {
    try {
        Statement stmt = conn.createStatement();
        
        // Insert employee
        stmt.executeUpdate(
            "INSERT INTO employees (empid, name, hourly_rate, job_title) " +
            "VALUES (999, 'Test User', 20.0, 'Tester')"
        );

        // Retrieve employee
        ResultSet rs = stmt.executeQuery("SELECT * FROM employees WHERE empid = 999");
        if (rs.next()) {
            assertEquals("Test User", rs.getString("name"));
            assertEquals(20.0, rs.getDouble("hourly_rate"), 0.01);
            assertEquals("Tester", rs.getString("job_title"));
        } else {
            fail("Inserted employee not found");
        }

        rs.close();

        // Cleanup
        stmt.executeUpdate("DELETE FROM employees WHERE empid = 999");
        stmt.close();

    } catch (SQLException e) {
        fail("SQL error: " + e.getMessage());
    }
}
}