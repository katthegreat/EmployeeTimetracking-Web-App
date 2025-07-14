package tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmployeeClockInTest {

    private static Connection conn;

    @BeforeAll
    public static void setupDatabase() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/timetracking", "root", "Mitakedame12");
    }

    @AfterAll
    public static void closeConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    @Test
public void testEmployeeClockIn() {
    int empid = 2;
    try {
        
        PreparedStatement ensureEmpStmt = conn.prepareStatement(
            "INSERT INTO employees (empid, name, hourly_rate, job_title) " +
            "VALUES (?, 'Test Employee', 18.50, 'Developer') " +
            "ON DUPLICATE KEY UPDATE name = name;"
        );
        ensureEmpStmt.setInt(1, empid);
        ensureEmpStmt.executeUpdate();
        ensureEmpStmt.close();

        PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO time_logs (empid, clock_in_time) VALUES (?, NOW())",
            Statement.RETURN_GENERATED_KEYS
        );
        insertStmt.setInt(1, empid);
        int affectedRows = insertStmt.executeUpdate();

        assertEquals(1, affectedRows, "Clock-in should insert 1 row");

       
        ResultSet keys = insertStmt.getGeneratedKeys();
        assertTrue(keys.next(), "Generated key should exist");

        int newId = keys.getInt(1);
        keys.close();
        insertStmt.close();

        PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM time_logs WHERE log_id = ?");
checkStmt.setInt(1, newId);

        ResultSet rs = checkStmt.executeQuery();

        assertTrue(rs.next(), "Clock-in record should exist");
        assertEquals(empid, rs.getInt("empid"), "Employee ID should match");

        rs.close();
        checkStmt.close();
        PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM time_logs WHERE log_id = ?");
deleteStmt.setInt(1, newId);
        deleteStmt.executeUpdate();
        deleteStmt.close();

    } catch (SQLException e) {
        fail("SQL Error: " + e.getMessage());
    }
}
}