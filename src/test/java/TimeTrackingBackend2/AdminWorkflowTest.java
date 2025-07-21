package TimeTrackingBackend2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class AdminWorkflowTest {
    private AdminService adminService;
    private Employee testEmployee;

    @BeforeEach
    public void setUp() {
        // Initialize with test data
        adminService = new AdminService();
        testEmployee = new Employee("EMP789", "Jane Smith", "Designer", true);
        adminService.addEmployee(testEmployee);
    }

    // Test 1: View Active Employees
    @Test
    public void testViewActiveEmployees() {
        // Add an inactive employee for contrast
        Employee inactiveEmployee = new Employee("EMP000", "Inactive", "Temp", false);
        adminService.addEmployee(inactiveEmployee);

        List<Employee> activeEmployees = adminService.getActiveEmployees();
        
        assertAll(
            () -> assertFalse(activeEmployees.isEmpty(), "Should return non-empty list"),
            () -> assertEquals(1, activeEmployees.size(), "Should return only active employees"),
            () -> assertEquals("EMP789", activeEmployees.get(0).getId(), "Should match test employee ID"),
            () -> assertTrue(activeEmployees.stream().noneMatch(e -> !e.isActive()), 
                "No inactive employees should appear"
        );
    }

    // Test 2: Update Employee Information
    @Test
    public void testUpdateEmployeeInformation() {
        // New data
        String updatedName = "Jane Doe";
        String updatedRole = "Senior Designer";
        
        // Perform update
        boolean updateResult = adminService.updateEmployee(
            testEmployee.getId(), 
            updatedName, 
            updatedRole
        );
        
        // Verify
        Employee updatedEmployee = adminService.getEmployeeById(testEmployee.getId());
        
        assertAll(
            () -> assertTrue(updateResult, "Update should return true on success"),
            () -> assertEquals(updatedName, updatedEmployee.getName(), "Name should update"),
            () -> assertEquals(updatedRole, updatedEmployee.getRole(), "Role should update"),
            () -> assertEquals(testEmployee.getId(), updatedEmployee.getId(), "ID should remain unchanged")
        );
    }

    @Test
    public void testUpdateNonExistentEmployee() {
        assertFalse(
            adminService.updateEmployee("BAD_ID", "Name", "Role"),
            "Should return false for invalid employee ID"
        );
    }
}