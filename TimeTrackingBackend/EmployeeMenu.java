package TimeTrackingBackend;

import java.util.Scanner;

public class EmployeeMenu {
    private String employeeName;

    public EmployeeMenu(String employeeName) {
        this.employeeName = employeeName;
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n=== Employee Menu ===");
            System.out.println("Welcome, " + employeeName + "!");
            System.out.println("1. Clock In");
            System.out.println("2. Clock Out");
            System.out.println("3. View Payroll Summary");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            
            choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    System.out.println("You have successfully clocked in.");
                    break;
                case 2:
                    System.out.println("You have successfully clocked out.");
                    break;
                case 3:
                    System.out.println("Displaying payroll summary...");
                    break;
                case 4:
                    System.out.println("Logging out...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 4);
    }
}
