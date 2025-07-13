package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;

import org.junit.jupiter.api.*;

public class EmployeeViewsPayrollTest {

    private static Connection conn;

    @BeforeAll
    public static void setupDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/timetracking",
                "root", // your DB username
                "Mitakedame12" // your DB password
            );
        } catch (Exception e) {
            fail("Database connection failed: " + e.getMessage());
        }
    }

    @AfterAll
    public static void closeDatabase() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
public void testViewPayrollSummary() {
    int empid = 2; // Use a valid employee ID that exists in payroll table

    try {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT total_hours, total_pay FROM payroll WHERE empid = ?"
        );
        stmt.setInt(1, empid);
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "Payroll entry should exist for employee");

        double totalHours = rs.getDouble("total_hours");
        double totalPay = rs.getDouble("total_pay");

        assertTrue(totalHours >= 0, "Total hours should be non-negative");
        assertTrue(totalPay >= 0, "Total pay should be non-negative");

        rs.close();
        stmt.close();
    } catch (SQLException e) {
        fail("SQL Error: " + e.getMessage());
    }
}
}