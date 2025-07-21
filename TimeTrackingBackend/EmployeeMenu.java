package TimeTrackingBackend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class EmployeeMenu {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd yyyy, hh:mm a");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void run(Connection conn, Scanner scanner, int empId) throws SQLException {
        while (true) {
            System.out.println("\n=== EMPLOYEE MENU ===");
            System.out.println("1. Punch In (with notes)");
            System.out.println("2. Punch Out");
            System.out.println("3. View Today's Time Log");
            System.out.println("4. View Weekly Hours");
            System.out.println("5. View Monthly Summary");
            System.out.println("6. Exit");
            System.out.print("Choose an option (1-6): ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        handlePunchIn(conn, scanner, empId);
                        break;
                    case "2":
                        punchOut(conn, empId);
                        break;
                    case "3":
                        viewTimeLogs(conn, empId, "today");
                        break;
                    case "4":
                        viewTimeLogs(conn, empId, "week");
                        break;
                    case "5":
                        viewTimeLogs(conn, empId, "month");
                        break;
                    case "6":
                        System.out.println("Logging out...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter 1-6.");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                logError(e);
            }
        }
    }

    private static void handlePunchIn(Connection conn, Scanner scanner, int empId) throws SQLException {
        if (hasActivePunch(conn, empId)) {
            System.out.println("⚠️ You already have an active punch-in. Please punch out first.");
            return;
        }

        System.out.print("Enter notes for this session (optional): ");
        String notes = scanner.nextLine();

        String insert = "INSERT INTO time_logs (empid, punch_in, notes) VALUES (?, NOW(), ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insert)) {
            stmt.setInt(1, empId);
            stmt.setString(2, notes.isEmpty() ? null : notes);
            stmt.executeUpdate();
            
            String query = "SELECT punch_in FROM time_logs WHERE empid = ? ORDER BY punch_in DESC LIMIT 1";
            try (PreparedStatement timeStmt = conn.prepareStatement(query)) {
                timeStmt.setInt(1, empId);
                ResultSet rs = timeStmt.executeQuery();
                if (rs.next()) {
                    Timestamp punchInTime = rs.getTimestamp("punch_in");
                    System.out.printf("✅ Punched in at: %s%n", 
                                    TIME_FORMAT.format(punchInTime.toLocalDateTime()));
                    if (notes != null && !notes.isEmpty()) {
                        System.out.println("📝 Notes: " + notes);
                    }
                }
            }
        }
    }

    private static void punchOut(Connection conn, int empId) throws SQLException {
        if (!hasActivePunch(conn, empId)) {
            System.out.println("⚠️ No active punch-in found. Please punch in first.");
            return;
        }

        String update = "UPDATE time_logs SET punch_out = NOW() " +
                       "WHERE empid = ? AND punch_out IS NULL " +
                       "ORDER BY punch_in DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(update, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, empId);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                String query = "SELECT t.punch_in, t.punch_out, t.notes, e.hourly_rate " +
                              "FROM time_logs t JOIN employees e ON t.empid = e.empid " +
                              "WHERE t.empid = ? ORDER BY t.punch_out DESC LIMIT 1";
                try (PreparedStatement timeStmt = conn.prepareStatement(query)) {
                    timeStmt.setInt(1, empId);
                    ResultSet rs = timeStmt.executeQuery();
                    if (rs.next()) {
                        Timestamp punchIn = rs.getTimestamp("punch_in");
                        Timestamp punchOut = rs.getTimestamp("punch_out");
                        long minutes = (punchOut.getTime() - punchIn.getTime()) / (60 * 1000);
                        double hours = minutes / 60.0;
                        double pay = hours * rs.getDouble("hourly_rate");
                        
                        System.out.printf("✅ Punched out at: %s%n", 
                                        TIME_FORMAT.format(punchOut.toLocalDateTime()));
                        System.out.printf("⏱️ Session duration: %d hours %d minutes%n", 
                                        minutes / 60, minutes % 60);
                        System.out.printf("💰 Estimated pay: $%.2f%n", pay);
                        
                        String notes = rs.getString("notes");
                        if (notes != null && !notes.isEmpty()) {
                            System.out.println("📝 Session notes: " + notes);
                        }
                    }
                }
            }
        }
    }

    private static void viewTimeLogs(Connection conn, int empId, String range) throws SQLException {
        String query;
        String title;
        
        switch (range.toLowerCase()) {
            case "today":
                query = "SELECT punch_in, punch_out, notes FROM time_logs " +
                        "WHERE empid = ? AND DATE(punch_in) = CURRENT_DATE() " +
                        "ORDER BY punch_in";
                title = "TODAY'S TIME LOG";
                break;
            case "week":
                query = "SELECT DATE(punch_in) as date, " +
                        "SEC_TO_TIME(SUM(TIMESTAMPDIFF(SECOND, punch_in, punch_out))) as total, " +
                        "COUNT(*) as entries " +
                        "FROM time_logs " +
                        "WHERE empid = ? AND punch_out IS NOT NULL " +
                        "AND punch_in >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY) " +
                        "GROUP BY DATE(punch_in) " +
                        "ORDER BY date";
                title = "WEEKLY SUMMARY";
                break;
            case "month":
                query = "SELECT DATE_FORMAT(punch_in, '%Y-%m-%d') as date, " +
                        "SEC_TO_TIME(SUM(TIMESTAMPDIFF(SECOND, punch_in, punch_out))) as total, " +
                        "COUNT(*) as entries " +
                        "FROM time_logs " +
                        "WHERE empid = ? AND punch_out IS NOT NULL " +
                        "AND punch_in >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH) " +
                        "GROUP BY DATE(punch_in) " +
                        "ORDER BY date";
                title = "MONTHLY SUMMARY";
                break;
            default:
                throw new SQLException("Invalid time range specified");
        }

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, empId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("No time entries found for this period.");
                return;
            }

            System.out.printf("\n=== %s ===%n", title);
            
            if (range.equals("today")) {
                System.out.printf("%-20s %-20s %-15s %-30s%n", 
                                 "Punch In", "Punch Out", "Duration", "Notes");
                System.out.println("------------------------------------------------------------------------");
                
                while (rs.next()) {
                    Timestamp in = rs.getTimestamp("punch_in");
                    Timestamp out = rs.getTimestamp("punch_out");
                    String notes = rs.getString("notes");
                    
                    String inTime = TIME_FORMAT.format(in.toLocalDateTime());
                    String outTime = out != null ? TIME_FORMAT.format(out.toLocalDateTime()) : "Active";
                    String duration = out != null ? formatDuration(in, out) : "Ongoing";
                    
                    System.out.printf("%-20s %-20s %-15s %-30s%n", 
                                    inTime, outTime, duration, 
                                    notes != null ? notes : "");
                }
            } else {
                System.out.printf("%-15s %-15s %-10s%n", "Date", "Total Hours", "Entries");
                System.out.println("-----------------------------------");
                
                while (rs.next()) {
                    String date = rs.getString("date");
                    String total = rs.getString("total");
                    int entries = rs.getInt("entries");
                    
                    System.out.printf("%-15s %-15s %-10d%n", date, total, entries);
                }
            }
        }
    }

    private static boolean hasActivePunch(Connection conn, int empId) throws SQLException {
        String check = "SELECT COUNT(*) FROM time_logs WHERE empid = ? AND punch_out IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(check)) {
            stmt.setInt(1, empId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static String formatDuration(Timestamp start, Timestamp end) {
        long minutes = (end.getTime() - start.getTime()) / (60 * 1000);
        return String.format("%dh %dm", minutes / 60, minutes % 60);
    }

    private static void logError(Exception e) {
        System.err.println("\n=== ERROR DETAILS ===");
        System.err.println("Time: " + LocalDateTime.now().format(DATE_FORMAT));
        System.err.println("Error: " + e.getMessage());
        System.err.println("Stack trace:");
        e.printStackTrace(System.err);
    }
}