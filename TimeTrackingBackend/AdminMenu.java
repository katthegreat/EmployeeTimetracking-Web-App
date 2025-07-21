package TimeTrackingBackend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class AdminMenu {

    public static void showAdminMenu(String adminName) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\nWelcome, " + adminName);
            System.out.println("Admin Menu:");
            System.out.println("1. View All Employees");
            System.out.println("2. View Clock-in Data");
            System.out.println("3. View Clock-out Data");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    viewAllEmployees();
                    break;
                case 2:
                    viewClockInData();
                    break;
                case 3:
                    viewClockOutData();
                    break;
                case 4:
                    System.out.println("Exiting admin menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

        } while (choice != 4);
    }

    private static void viewAllEmployees() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/timetracking", "root", "");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM employee");

            System.out.println("\n--- All Employees ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                                   ", Name: " + rs.getString("name") +
                                   ", Role: " + rs.getString("role"));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error viewing employees: " + e.getMessage());
        }
    }

    private static void viewClockInData() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/timetracking", "root", "");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM clockin");

            System.out.println("\n--- Clock-in Data ---");
            while (rs.next()) {
                System.out.println("Employee ID: " + rs.getInt("employee_id") +
                                   ", Time: " + rs.getTimestamp("clockin_time"));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error viewing clock-in data: " + e.getMessage());
        }
    }

    private static void viewClockOutData() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/timetracking", "root", "");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM clockout");

            System.out.println("\n--- Clock-out Data ---");
            while (rs.next()) {
                System.out.println("Employee ID: " + rs.getInt("employee_id") +
                                   ", Time: " + rs.getTimestamp("clockout_time"));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error viewing clock-out data: " + e.getMessage());
        }
    }
}
