# EmployeeTimetracking-Web-App
A way to manage employee Clock-in and Clock-out Times along with other employee information. 







## Unit Tests

The `AdminMenuTest.java` file contains unit tests for the Admin Menu features of the Time Tracking Web App. These tests are written using JUnit 5 and verify key functionalities like:

- Viewing all employees  
- Updating an employee's job title  
- Updating an employee's hourly rate  
- Inserting and retrieving a new employee  

The tests connect to a local MySQL `timetracking` database and check for data consistency using assertions. All tests have been confirmed to pass successfully.


### ✅ New Tests for Employees

Two more tests were added:

- Clock-In: Saves clock-in time to `time_logs`
- Payroll Summary: Gets payroll info from `payroll` table
