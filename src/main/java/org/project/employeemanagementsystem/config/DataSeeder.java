package org.project.employeemanagementsystem.config;


import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.model.LeaveStatus; // Enum import
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final HolidayRepository holidayRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PaymentRepository paymentRepository;
    private final SystemLogRepository systemLogRepository;
    private final PasswordEncoder passwordEncoder;

    // Random generator for realistic variations
    private final Random random = new Random();

    public DataSeeder(UserRepository userRepository, RoleRepository roleRepository,
                      DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                      LeaveTypeRepository leaveTypeRepository, LeaveRequestRepository leaveRequestRepository,
                      AttendanceRepository attendanceRepository, ScheduleRepository scheduleRepository,
                      HolidayRepository holidayRepository, SystemSettingRepository systemSettingRepository,
                      PaymentRepository paymentRepository, SystemLogRepository systemLogRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.scheduleRepository = scheduleRepository;
        this.holidayRepository = holidayRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.paymentRepository = paymentRepository;
        this.systemLogRepository = systemLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Αν υπάρχουν χρήστες, σταματάμε για αποφυγή διπλοτύπων
        if (userRepository.count() > 0) return;

        System.out.println(">>> SEEDING DATA START...");

        // ==========================================
        // 1. SYSTEM SETTINGS & HOLIDAYS & ROLES
        // ==========================================
        seedSettings();
        seedHolidays();
        List<Role> roles = seedRoles();
        Role adminRole = roles.get(0);
        Role userRole = roles.get(1);

        // ==========================================
        // 2. DEPARTMENTS & LEAVE TYPES
        // ==========================================
        List<Department> depts = seedDepartments();
        List<LeaveType> leaveTypes = seedLeaveTypes();

        // ==========================================
        // 3. ADMIN USER (The "a" / "1" account)
        // ==========================================
        User adminUser = new User();
        adminUser.setUsername("a");
        adminUser.setPassword(passwordEncoder.encode("1"));
        adminUser.setRole(adminRole);
        // Save handled via cascading in Employee, but we need object ref

        Employee adminEmployee = new Employee();
        adminEmployee.setFirstName("System");
        adminEmployee.setLastName("Admin");
        adminEmployee.setEmail("admin@ems.com");
        adminEmployee.setPhone("6900000000");
        adminEmployee.setAddress("Headquarters");
        adminEmployee.setSsn("SSN-ADMIN-001");
        adminEmployee.setHireDate(LocalDate.now().minusYears(5));
        adminEmployee.setSalary(5000.0);
        adminEmployee.setDepartment(depts.get(0)); // IT Dept
        adminEmployee.setUser(adminUser);
        adminUser.setEmployee(adminEmployee);

        employeeRepository.save(adminEmployee);

        // Log login for admin
        logAction(adminUser, "LOGIN", LocalDateTime.now().minusMinutes(10));


        // ==========================================
        // 4. BULK EMPLOYEES & USERS
        // ==========================================
        List<Employee> allEmployees = new ArrayList<>();
        allEmployees.add(adminEmployee);

        // Create 10 more employees
        allEmployees.add(createEmployee("John", "Doe", "john@ems.com", "6911111111", "SSN-002", 2500.0, depts.get(0), userRole, "user1", "123")); // IT
        allEmployees.add(createEmployee("Jane", "Smith", "jane@ems.com", "6922222222", "SSN-003", 2600.0, depts.get(1), userRole, "user2", "123")); // HR
        allEmployees.add(createEmployee("Mike", "Brown", "mike@ems.com", "6933333333", "SSN-004", 1800.0, depts.get(2), userRole, "user3", "123")); // Sales
        allEmployees.add(createEmployee("Emily", "Davis", "emily@ems.com", "6944444444", "SSN-005", 1900.0, depts.get(2), userRole, "user4", "123")); // Sales
        allEmployees.add(createEmployee("Chris", "Wilson", "chris@ems.com", "6955555555", "SSN-006", 3200.0, depts.get(0), userRole, "user5", "123")); // IT
        allEmployees.add(createEmployee("Anna", "Taylor", "anna@ems.com", "6966666666", "SSN-007", 2100.0, depts.get(1), userRole, "user6", "123")); // HR
        allEmployees.add(createEmployee("David", "Moore", "david@ems.com", "6977777777", "SSN-008", 4000.0, depts.get(3), adminRole, "manager1", "123")); // Management
        allEmployees.add(createEmployee("Sarah", "Anderson", "sarah@ems.com", "6988888888", "SSN-009", 1500.0, depts.get(2), userRole, "user7", "123")); // Sales
        allEmployees.add(createEmployee("Kevin", "Thomas", "kevin@ems.com", "6999999999", "SSN-010", 2800.0, depts.get(0), userRole, "user8", "123")); // IT
        allEmployees.add(createEmployee("Laura", "Jackson", "laura@ems.com", "6900000001", "SSN-011", 2400.0, depts.get(1), userRole, "user9", "123")); // HR

        // ==========================================
        // 5. SCHEDULES (Last 30 days + Next 7 days)
        // ==========================================
        seedSchedules(allEmployees);

        // ==========================================
        // 6. ATTENDANCES (Last 30 days)
        // ==========================================
        seedAttendances(allEmployees);

        // ==========================================
        // 7. LEAVE REQUESTS
        // ==========================================
        seedLeaveRequests(allEmployees, leaveTypes);

        // ==========================================
        // 8. PAYMENTS (Last Month)
        // ==========================================
        seedPayments(allEmployees);

        System.out.println(">>> SEEDING COMPLETED SUCCESSFULLY.");
        System.out.println(">>> Main Admin: a / 1");
    }

    // --- HELPER METHODS ---

    private void seedSettings() {
        systemSettingRepository.save(new SystemSetting("payroll.tax_rate_employer", "0.30"));
        systemSettingRepository.save(new SystemSetting("payroll.tax_rate_employee", "0.15"));
        systemSettingRepository.save(new SystemSetting("company.name", "EMS Solutions Ltd"));
        systemSettingRepository.save(new SystemSetting("company.currency", "EUR"));
    }

    private void seedHolidays() {
        int year = LocalDate.now().getYear();
        holidayRepository.save(new Holiday(null, "New Year's Day", LocalDate.of(year, 1, 1)));
        holidayRepository.save(new Holiday(null, "Epiphany", LocalDate.of(year, 1, 6)));
        holidayRepository.save(new Holiday(null, "Independence Day", LocalDate.of(year, 3, 25)));
        holidayRepository.save(new Holiday(null, "Labor Day", LocalDate.of(year, 5, 1)));
        holidayRepository.save(new Holiday(null, "Christmas", LocalDate.of(year, 12, 25)));
    }

    private List<Role> seedRoles() {
        Role admin = new Role(null, "ROLE_ADMIN", "Full access to the system");
        Role user = new Role(null, "ROLE_USER", "Standard employee access");
        roleRepository.saveAll(Arrays.asList(admin, user));
        return Arrays.asList(admin, user);
    }

    private List<Department> seedDepartments() {
        Department it = new Department(null, "IT", "Information Technology", null);
        Department hr = new Department(null, "HR", "Human Resources", null);
        Department sales = new Department(null, "Sales", "Sales and Marketing", null);
        Department mgmt = new Department(null, "Management", "Executive Board", null);
        departmentRepository.saveAll(Arrays.asList(it, hr, sales, mgmt));
        return Arrays.asList(it, hr, sales, mgmt);
    }

    private List<LeaveType> seedLeaveTypes() {
        LeaveType annual = new LeaveType(null, "Annual Leave", 20);
        LeaveType sick = new LeaveType(null, "Sick Leave", 15);
        LeaveType unpaid = new LeaveType(null, "Unpaid Leave", 365);
        LeaveType maternity = new LeaveType(null, "Maternity Leave", 120);
        leaveTypeRepository.saveAll(Arrays.asList(annual, sick, unpaid, maternity));
        return Arrays.asList(annual, sick, unpaid, maternity);
    }

    private Employee createEmployee(String first, String last, String email, String phone, String ssn, Double salary, Department dept, Role role, String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        Employee emp = new Employee();
        emp.setFirstName(first);
        emp.setLastName(last);
        emp.setEmail(email);
        emp.setPhone(phone);
        emp.setAddress("Sample Address " + random.nextInt(100));
        emp.setSsn(ssn);
        emp.setHireDate(LocalDate.now().minusMonths(random.nextInt(24) + 1)); // Hired 1-24 months ago
        emp.setSalary(salary);
        emp.setDepartment(dept);

        emp.setUser(user);
        user.setEmployee(emp);

        return employeeRepository.save(emp);
    }

    private void seedSchedules(List<Employee> employees) {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(7);

        for (Employee emp : employees) {
            for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                // Skip weekends
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

                Schedule schedule = new Schedule();
                schedule.setEmployee(emp);
                schedule.setDate(date);
                schedule.setStartTime(LocalTime.of(9, 0));
                schedule.setEndTime(LocalTime.of(17, 0));
                schedule.setShiftName("Morning Standard");
                scheduleRepository.save(schedule);
            }
        }
    }

    private void seedAttendances(List<Employee> employees) {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (Employee emp : employees) {
            for (LocalDate date = startDate; date.isBefore(yesterday); date = date.plusDays(1)) {
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

                // 10% chance employee was absent/on leave (no attendance record)
                if (random.nextInt(10) == 0) continue;

                Attendance att = new Attendance();
                att.setEmployee(emp);
                att.setDate(date);

                // Add random variation to check-in (8:50 - 9:30)
                int randomMinutes = random.nextInt(40) - 10;
                att.setCheckInTime(LocalTime.of(9, 0).plusMinutes(randomMinutes));

                // Add random variation to check-out (16:50 - 17:30)
                int randomOut = random.nextInt(40) - 10;
                att.setCheckOutTime(LocalTime.of(17, 0).plusMinutes(randomOut));

                attendanceRepository.save(att);
            }
        }
    }

    private void seedLeaveRequests(List<Employee> employees, List<LeaveType> types) {
        // Create random requests
        for (Employee emp : employees) {
            if (emp.getId() % 2 == 0) { // Create requests for half the employees
                LeaveRequest req = new LeaveRequest();
                req.setEmployee(emp);
                req.setLeaveType(types.get(random.nextInt(types.size())));

                LocalDate start = LocalDate.now().minusDays(random.nextInt(60));
                req.setStartDate(start);
                req.setEndDate(start.plusDays(random.nextInt(5) + 1));
                req.setReason("Personal / Medical reasons");

                // Random status
                int statusRand = random.nextInt(3);
                if (statusRand == 0) req.setStatus(LeaveStatus.APPROVED);
                else if (statusRand == 1) req.setStatus(LeaveStatus.PENDING);
                else req.setStatus(LeaveStatus.REJECTED);

                leaveRequestRepository.save(req);
            }
        }
    }

    private void seedPayments(List<Employee> employees) {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String monthYear = lastMonth.getMonth().toString() + "-" + lastMonth.getYear();

        for (Employee emp : employees) {
            Payment p = new Payment();
            p.setEmployee(emp);
            p.setPaymentDate(LocalDate.now().withDayOfMonth(1)); // Paid on 1st of current month
            p.setMonthYear(monthYear);

            double base = emp.getSalary();
            p.setBaseSalary(base);
            p.setHoursWorked(160.0); // Standard month
            p.setOvertimeHours(random.nextDouble() * 10); // 0-10 hours OT
            p.setSundayHours(0.0);

            // Calculations (Simplified)
            double otPay = p.getOvertimeHours() * (base / 160 * 1.5);
            p.setGrossPay(base + otPay);

            p.setDeductions(p.getGrossPay() * 0.15); // 15% Employee Share
            p.setEmployerTax(p.getGrossPay() * 0.30); // 30% Employer Share
            p.setTotalTax(p.getDeductions() + p.getEmployerTax());

            p.setBonus(random.nextInt(5) == 0 ? 200.0 : 0.0); // Random bonus
            p.setAmount(p.getGrossPay() - p.getDeductions() + p.getBonus()); // Net Pay

            p.setStatus("COMPLETED");

            paymentRepository.save(p);
        }
    }

    private void logAction(User user, String action, LocalDateTime time) {
        SystemLog log = new SystemLog();
        log.setUser(user);
        log.setUsername(user.getUsername());
        log.setAction(action);
        log.setTimestamp(time);
        systemLogRepository.save(log);
    }
}