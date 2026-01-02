package org.project.employeemanagementsystem.config;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional; <--- ΑΦΑΙΡΕΣΗ ΑΥΤΟΥ

import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    public DataSeeder(UserRepository userRepository, RoleRepository roleRepository,
                      DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                      LeaveTypeRepository leaveTypeRepository, LeaveRequestRepository leaveRequestRepository,
                      AttendanceRepository attendanceRepository, ScheduleRepository scheduleRepository,
                      HolidayRepository holidayRepository, SystemSettingRepository systemSettingRepository,
                      PaymentRepository paymentRepository,
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
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    // @Transactional  <--- ΣΗΜΑΝΤΙΚΟ: ΤΟ ΑΦΑΙΡΕΣΑΜΕ ΓΙΑ ΝΑ ΜΗΝ ΧΤΥΠΑΕΙ Ο AUDIT LISTENER
    public void run(String... args) throws Exception {
        // Safe check: If users exist, assume DB is seeded
        if (userRepository.count() > 0) return;

        System.out.println(">>> SEEDING DATA START (ROLES: Admin, HR, Accountant, User)...");

        // 1. SETTINGS & HOLIDAYS
        seedSettings();
        seedHolidays();

        // 2. ROLES
        List<Role> roles = seedRoles();
        Role adminRole = roles.get(0);
        Role hrRole = roles.get(1);
        Role accountantRole = roles.get(2);
        Role userRole = roles.get(3);

        // 3. DEPARTMENTS & LEAVE TYPES
        List<Department> depts = seedDepartments();
        List<LeaveType> leaveTypes = seedLeaveTypes();

        // 4. MAIN ADMIN USER
        if (userRepository.findByUsername("a").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("a");
            adminUser.setPassword(passwordEncoder.encode("1"));
            adminUser.setRole(adminRole);

            Employee adminEmployee = new Employee();
            adminEmployee.setFirstName("System");
            adminEmployee.setLastName("Admin");
            adminEmployee.setEmail("admin@ems.com");
            adminEmployee.setPhone("6900000000");
            adminEmployee.setAddress("HQ");
            adminEmployee.setSsn("ADM-001");
            adminEmployee.setHireDate(LocalDate.now().minusYears(5));
            adminEmployee.setSalary(5000.0);
            adminEmployee.setDepartment(depts.get(0)); // IT
            adminEmployee.setUser(adminUser);
            adminUser.setEmployee(adminEmployee);

            employeeRepository.save(adminEmployee);
        }

        // 5. BULK EMPLOYEES
        // Επειδή βγάλαμε το @Transactional, πρέπει να ξαναβρούμε τον Admin αν χρειαστεί,
        // αλλά εδώ φτιάχνουμε νέους, οπότε δεν πειράζει.

        List<Employee> createdEmployees = new ArrayList<>();
        // Δεν προσθέτουμε τον admin στη λίστα για generation για να μην μπλέξουμε τα transactions

        // -- HR Employee --
        createdEmployees.add(createEmployee("Helen", "Robinson", "helen@ems.com", "6911111111", "HR-001",
                2800.0, depts.get(1), hrRole, "hr_user", "123"));

        // -- Accountant Employee --
        createdEmployees.add(createEmployee("Alice", "Finance", "alice@ems.com", "6922222222", "ACC-001",
                3000.0, depts.get(3), accountantRole, "acc_user", "123"));

        // -- Standard Users --
        createdEmployees.add(createEmployee("John", "Doe", "john@ems.com", "6933333333", "USR-001",
                2500.0, depts.get(0), userRole, "user1", "123"));

        createdEmployees.add(createEmployee("Mike", "Salesman", "mike@ems.com", "6944444444", "USR-002",
                1800.0, depts.get(2), userRole, "user2", "123"));

        createdEmployees.add(createEmployee("Sarah", "Connor", "sarah@ems.com", "6955555555", "USR-003",
                2200.0, depts.get(2), userRole, "user3", "123"));

        createdEmployees.add(createEmployee("Chris", "Tech", "chris@ems.com", "6966666666", "USR-004",
                3200.0, depts.get(0), userRole, "user4", "123"));

        createdEmployees.add(createEmployee("Peter", "Manager", "peter@ems.com", "6977777777", "MGR-001",
                4500.0, depts.get(3), adminRole, "manager1", "123"));

        // 6. DATA GENERATION
        seedSchedules(createdEmployees);
        seedAttendances(createdEmployees);
        seedLeaveRequests(createdEmployees, leaveTypes);
        seedPayments(createdEmployees);

        System.out.println(">>> SEEDING COMPLETED.");
    }

    // --- HELPER METHODS ---

    private void seedSettings() {
        // Σώζουμε ένα-ένα για να μην σκάσει το batch insert με τον AuditListener
        createSetting("payroll.standard_hours", "176.0");
        createSetting("payroll.overtime_rate", "1.5");
        createSetting("payroll.sunday_rate", "1.75");
        createSetting("payroll.night_rate", "1.25");
        createSetting("payroll.holiday_rate", "2.00");
        createSetting("payroll.total_tax_rate", "0.40");
        createSetting("payroll.employer_share", "0.60");
        createSetting("company.name", "EMS Solutions Ltd");
        createSetting("company.currency", "€");
    }

    private void createSetting(String key, String value) {
        if (!systemSettingRepository.existsById(key)) {
            systemSettingRepository.save(new SystemSetting(key, value));
        }
    }

    private void seedHolidays() {
        int year = LocalDate.now().getYear();
        if (holidayRepository.count() == 0) {
            holidayRepository.saveAll(Arrays.asList(
                    new Holiday(null, "New Year", LocalDate.of(year, 1, 1)),
                    new Holiday(null, "Labor Day", LocalDate.of(year, 5, 1)),
                    new Holiday(null, "Christmas", LocalDate.of(year, 12, 25))
            ));
        }
    }

    private List<Role> seedRoles() {
        if (roleRepository.count() > 0) return roleRepository.findAll();

        Role admin = new Role(null, "Admin", "Full Access");
        Role hr = new Role(null, "HR", "Employee Management");
        Role accountant = new Role(null, "Accountant", "Payroll & Finance");
        Role user = new Role(null, "User", "Employee Self Service");
        return roleRepository.saveAll(Arrays.asList(admin, hr, accountant, user));
    }

    private List<Department> seedDepartments() {
        if (departmentRepository.count() > 0) return departmentRepository.findAll();

        Department it = new Department(null, "IT", "Tech Dept", null);
        Department hr = new Department(null, "HR", "Human Resources", null);
        Department sales = new Department(null, "Sales", "Marketing", null);
        Department finance = new Department(null, "Finance", "Accounting", null);
        return departmentRepository.saveAll(Arrays.asList(it, hr, sales, finance));
    }

    private List<LeaveType> seedLeaveTypes() {
        if (leaveTypeRepository.count() > 0) return leaveTypeRepository.findAll();

        LeaveType annual = new LeaveType(null, "Annual", 20);
        LeaveType sick = new LeaveType(null, "Sick", 15);
        return leaveTypeRepository.saveAll(Arrays.asList(annual, sick));
    }

    private Employee createEmployee(String first, String last, String email, String phone, String ssn,
                                    Double salary, Department dept, Role role, String username, String password) {
        // Ελέγχουμε αν υπάρχει ήδη ο χρήστης για αποφυγή duplicate entry
        if (userRepository.findByUsername(username).isPresent()) {
            return employeeRepository.findByEmail(email).orElse(null);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        Employee emp = new Employee();
        emp.setFirstName(first);
        emp.setLastName(last);
        emp.setEmail(email);
        emp.setPhone(phone);
        emp.setAddress("Street " + random.nextInt(100));
        emp.setSsn(ssn);
        emp.setHireDate(LocalDate.now().minusMonths(random.nextInt(24) + 1));
        emp.setSalary(salary);
        emp.setDepartment(dept);

        emp.setUser(user);
        user.setEmployee(emp);

        return employeeRepository.save(emp);
    }

    private void seedSchedules(List<Employee> employees) {
        if (scheduleRepository.count() > 0) return;

        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(7);
        List<Schedule> schedules = new ArrayList<>();

        for (Employee emp : employees) {
            if (emp == null) continue; // Safety check
            for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                Schedule s = new Schedule();
                s.setEmployee(emp);
                s.setDate(date);
                s.setStartTime(LocalTime.of(9, 0));
                s.setEndTime(LocalTime.of(17, 0));
                s.setShiftName("Standard");
                schedules.add(s);
            }
        }
        scheduleRepository.saveAll(schedules);
    }

    private void seedAttendances(List<Employee> employees) {
        if (attendanceRepository.count() > 0) return;

        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Attendance> attendances = new ArrayList<>();

        for (Employee emp : employees) {
            if (emp == null) continue;
            for (LocalDate date = startDate; date.isBefore(yesterday); date = date.plusDays(1)) {
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                if (random.nextInt(10) == 0) continue; // 10% absent

                Attendance a = new Attendance();
                a.setEmployee(emp);
                a.setDate(date);
                a.setCheckInTime(LocalTime.of(9, 0).plusMinutes(random.nextInt(30) - 10));
                a.setCheckOutTime(LocalTime.of(17, 0).plusMinutes(random.nextInt(30) - 10));
                attendances.add(a);
            }
        }
        attendanceRepository.saveAll(attendances);
    }

    private void seedLeaveRequests(List<Employee> employees, List<LeaveType> types) {
        if (leaveRequestRepository.count() > 0) return;

        List<LeaveRequest> requests = new ArrayList<>();
        for (Employee emp : employees) {
            if (emp == null) continue;
            if (random.nextBoolean()) {
                LeaveRequest req = new LeaveRequest();
                req.setEmployee(emp);
                req.setLeaveType(types.get(random.nextInt(types.size())));
                req.setStartDate(LocalDate.now().minusDays(random.nextInt(30)));
                req.setEndDate(req.getStartDate().plusDays(2));
                req.setReason("Test Leave");
                req.setStatus(LeaveStatus.APPROVED);
                requests.add(req);
            }
        }
        leaveRequestRepository.saveAll(requests);
    }

    private void seedPayments(List<Employee> employees) {
        if (paymentRepository.count() > 0) return;

        String month = LocalDate.now().minusMonths(1).getMonth().toString();
        List<Payment> payments = new ArrayList<>();

        for (Employee emp : employees) {
            if (emp == null) continue;
            Payment p = new Payment();
            p.setEmployee(emp);
            p.setMonthYear(month + "-" + LocalDate.now().getYear());
            p.setPaymentDate(LocalDate.now().withDayOfMonth(1));
            p.setBaseSalary(emp.getSalary());
            p.setGrossPay(emp.getSalary());
            p.setDeductions(emp.getSalary() * 0.15);
            p.setEmployerTax(emp.getSalary() * 0.25);
            p.setTotalTax(p.getDeductions() + p.getEmployerTax());
            p.setAmount(p.getGrossPay() - p.getDeductions());
            p.setStatus("COMPLETED");
            payments.add(p);
        }
        paymentRepository.saveAll(payments);
    }
}