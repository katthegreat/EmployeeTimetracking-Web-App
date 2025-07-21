package TimeTrackingBackend;

public class PayrollReport {

    private String employeeName;
    private double hoursWorked;
    private double hourlyRate;

    public PayrollReport(String employeeName, double hoursWorked, double hourlyRate) {
        this.employeeName = employeeName;
        this.hoursWorked = hoursWorked;
        this.hourlyRate = hourlyRate;
    }

    public void generateReport() {
        double grossPay = hoursWorked * hourlyRate;
        System.out.println("=== Payroll Report ===");
        System.out.println("Employee: " + employeeName);
        System.out.println("Hours Worked: " + hoursWorked);
        System.out.println("Hourly Rate: $" + hourlyRate);
        System.out.println("Gross Pay: $" + grossPay);
    }
}
