package TimeTrackingBackend;

import java.sql.*;
import java.time.*;
import java.util.Random;

public class TestDataGenerator {
    public static void main(String[] args) {
        String dbUrl = "jdbc:mysql://localhost:3306/timetracking";
        String dbUser = "root";
        String dbPassword = "YourPasswordHere"; // 👈 update with your real password

        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // ✅ register JDBC driver

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                System.out.println("Connected to DB");

                int[] employeeIds = {1, 2, 3}; // ✅ update with real empids
                LocalDate startDate = LocalDate.of(2024, 7, 1);
                LocalDate endDate = LocalDate.of(2025, 7, 20);
                Random random = new Random();

                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO time_logs (empid, punch_in, punch_out) VALUES (?, ?, ?)");

                for (int empid : employeeIds) {
                    LocalDate date = startDate;

                    while (!date.isAfter(endDate)) {
                        DayOfWeek day = date.getDayOfWeek();
                        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                            LocalTime inTime = LocalTime.of(9, random.nextInt(30)); // 9:00–9:29
                            LocalTime outTime = inTime.plusHours(8).plusMinutes(random.nextInt(30));
                            Timestamp punchIn = Timestamp.valueOf(LocalDateTime.of(date, inTime));
                            Timestamp punchOut = Timestamp.valueOf(LocalDateTime.of(date, outTime));

                            stmt.setInt(1, empid);
                            stmt.setTimestamp(2, punchIn);
                            stmt.setTimestamp(3, punchOut);
                            stmt.addBatch();
                        }
                        date = date.plusDays(1);
                    }
                }

                stmt.executeBatch();
                System.out.println("✅ Test time log data inserted.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
