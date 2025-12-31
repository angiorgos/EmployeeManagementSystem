package org.project.employeemanagementsystem;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // <--- ΣΗΜΑΝΤΙΚΟ
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PaymentRepository paymentRepository;
    private final ScheduleRepository scheduleRepository;
    private final SystemLogRepository systemLogRepository;

    // Προσθήκη του Encoder
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository,
                      DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                      AttendanceRepository attendanceRepository, HolidayRepository holidayRepository,
                      LeaveTypeRepository leaveTypeRepository, LeaveRequestRepository leaveRequestRepository,
                      PaymentRepository paymentRepository, ScheduleRepository scheduleRepository,
                      SystemLogRepository systemLogRepository,
                      PasswordEncoder passwordEncoder) { // <--- Inject εδώ
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidayRepository = holidayRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.paymentRepository = paymentRepository;
        this.scheduleRepository = scheduleRepository;
        this.systemLogRepository = systemLogRepository;
        this.passwordEncoder = passwordEncoder; // <--- Ανάθεση
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            System.out.println("--- 🚀 STARTING DATABASE SEEDING (SECURE MODE) ---");

            // 1. ROLES
            Role adminRole = new Role(); adminRole.setName("ADMIN"); adminRole.setDescription("System Administrator");
            Role userRole = new Role(); userRole.setName("USER"); userRole.setDescription("Standard User");
            roleRepository.saveAll(Arrays.asList(adminRole, userRole));

            // 2. ADMIN USER
            User admin = new User();
            admin.setUsername("admin");
            // ΕΔΩ Η ΑΛΛΑΓΗ: Κρυπτογράφηση του κωδικού
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setRole(adminRole);
            userRepository.save(admin);
            System.out.println("✅ Admin User Created (Pass: 1234)");

            // 3. DEPARTMENTS
            Department it = new Department(); it.setName("IT"); it.setDescription("Technology Dept");
            departmentRepository.save(it);

            Department hr = new Department(); hr.setName("HR"); hr.setDescription("Human Resources");
            departmentRepository.save(hr);

            Department sales = new Department(); sales.setName("Sales"); sales.setDescription("Sales Dept");
            departmentRepository.save(sales);

            // 4. HOLIDAYS
            int year = LocalDate.now().getYear();
            createHoliday("New Year", LocalDate.of(year, 1, 1));
            createHoliday("Christmas", LocalDate.of(year, 12, 25));

            // 5. LEAVE TYPES
            createLeaveType("Annual", 20);
            createLeaveType("Sick", 10);

            // 6. EMPLOYEES
            Employee emp1 = createEmployee("Georgios", "Papadopoulos", "g.pap@company.com", "6971111111", "100200300", 1800.0, it);
            Employee emp2 = createEmployee("Maria", "Dimitriou", "m.dim@company.com", "6972222222", "999888777", 1400.0, hr);
            Employee emp3 = createEmployee("Nikos", "Alexiou", "n.alex@company.com", "6973333333", "555666444", 1500.0, sales);

            // 7. USERS FOR EMPLOYEES
            createUserForEmployee("gpapadopoulos", userRole);
            createUserForEmployee("mdimitriou", userRole);

            // 8. SCHEDULES
            createScheduleForWeek(emp1, "Morning", LocalTime.of(9, 0), LocalTime.of(17, 0));
            createScheduleForWeek(emp2, "Morning", LocalTime.of(9, 0), LocalTime.of(17, 0));
            createScheduleForWeek(emp3, "Evening", LocalTime.of(14, 0), LocalTime.of(22, 0));

            // 9. ATTENDANCE & PAYMENTS & LOGS
            generateAttendanceForLastWeek(emp1, LocalTime.of(8, 55), LocalTime.of(17, 10));
            createHistoricalPayment(emp1, 1800.0);
            createLog("admin", "System initialized via Seeder");

            System.out.println("✅ Database Seeded Successfully!");
        }
    }

    // --- HELPER METHODS ---

    private Employee createEmployee(String first, String last, String email, String phone, String ssn, Double salary, Department dept) {
        Employee emp = new Employee();
        emp.setFirstName(first);
        emp.setLastName(last);
        emp.setEmail(email);
        emp.setPhone(phone);
        emp.setAddress("Greece");
        emp.setSsn(ssn);
        emp.setSalary(salary);
        emp.setDepartment(dept);
        emp.setHireDate(LocalDate.now().minusYears(1));
        return employeeRepository.save(emp);
    }

    private void createScheduleForWeek(Employee emp, String shiftName, LocalTime start, LocalTime end) {
        LocalDate date = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            if (date.getDayOfWeek().getValue() <= 5) {
                Schedule s = new Schedule();
                s.setEmployee(emp);
                s.setDate(date);
                s.setShiftName(shiftName);
                s.setStartTime(start);
                s.setEndTime(end);
                scheduleRepository.save(s);
            }
            date = date.plusDays(1);
        }
    }

    private void createUserForEmployee(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        // ΕΔΩ Η ΑΛΛΑΓΗ: Κρυπτογράφηση και για τους υπαλλήλους
        user.setPassword(passwordEncoder.encode("1234"));
        user.setRole(role);
        userRepository.save(user);
    }

    private void generateAttendanceForLastWeek(Employee emp, LocalTime in, LocalTime out) {
        LocalDate date = LocalDate.now().minusDays(5);
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            if (date.getDayOfWeek().getValue() <= 5) {
                Attendance att = new Attendance();
                att.setEmployee(emp);
                att.setDate(date);
                att.setCheckInTime(in.plusMinutes(random.nextInt(20) - 10));
                att.setCheckOutTime(out.plusMinutes(random.nextInt(20) - 10));
                attendanceRepository.save(att);
            }
            date = date.plusDays(1);
        }
    }

    private void createHistoricalPayment(Employee emp, Double salary) {
        Payment p = new Payment();
        p.setEmployee(emp);
        p.setPaymentDate(LocalDate.now().minusMonths(1));
        p.setMonthYear(LocalDate.now().minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")));
        p.setBaseSalary(salary);
        p.setGrossPay(salary);
        p.setEmployerTax(salary * 0.20);
        p.setDeductions(salary * 0.15);
        p.setAmount(salary * 0.65);
        p.setStatus("PAID");
        paymentRepository.save(p);
    }

    private void createHoliday(String name, LocalDate date) {
        Holiday h = new Holiday();
        h.setName(name);
        h.setDate(date);
        holidayRepository.save(h);
    }

    private void createLeaveType(String name, int days) {
        LeaveType lt = new LeaveType();
        lt.setName(name);
        lt.setMaxDays(days);
        leaveTypeRepository.save(lt);
    }

    private void createLog(String username, String action) {
        SystemLog log = new SystemLog();
        log.setUsername(username);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());
        systemLogRepository.save(log);
    }
}