package TimeTrackingBackend;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;

public class EmployeeAuthTest {
    private Connection conn;
    private final String DB_URL = "jdbc:mysql://localhost:3306/timetracking?useSSL=false";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "YourNewPassword123!";

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("Connected to database: " + conn.getCatalog());
    }

    @Test
    @DisplayName("Verify Snoopy exists with correct data")
    void testSnoopyRecord() throws SQLException {
        String sql = "SELECT * FROM employees WHERE first_name='Snoopy' AND last_name='Beagle'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        
        assertTrue(rs.next(), "Snoopy Beagle should exist in employees table");
        assertEquals(25.00, rs.getDouble("hourly_rate"), 0.01, "Hourly rate should be 25.00");
        assertEquals("Software Engineer", rs.getString("job_title"), "Job title should match");
        
        // Verify there's only one Snoopy
        assertFalse(rs.next(), "There should be only one Snoopy Beagle record");
    }

    @Test
    @DisplayName("Verify user credentials")
    void testUserCredentials() throws SQLException {
        String sql = "SELECT e.* FROM users u " +
                    "JOIN employees e ON u.employee_id = e.employee_id " +
                    "WHERE u.username='snoopy' AND u.password='emppass'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        
        assertTrue(rs.next(), "Valid credentials should return employee record");
        assertEquals("Snoopy", rs.getString("first_name"), "First name should match");
        assertEquals("Beagle", rs.getString("last_name"), "Last name should match");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null) conn.close();
    }
}
