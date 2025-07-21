package TimeTrackingBackend;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        String envCheck = dotenv.get("DB_URL");
        if (envCheck != null) {
            System.out.println("✅ Environment variables loaded successfully!");
        } else {
            System.out.println("⚠️ Failed to load environment variables.");
        }

        PayrollReport report = new PayrollReport("Alex Johnson", 40, 25.00);
        report.generateReport();
    }
}
