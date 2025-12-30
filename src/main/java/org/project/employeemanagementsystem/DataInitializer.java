package org.project.employeemanagementsystem;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    // Όλα τα Repositories
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final ScheduleRepository scheduleRepository; // ΝΕΟ
    private final PaymentRepository paymentRepository;   // ΝΕΟ
    private final SystemLogRepository systemLogRepository; // ΝΕΟ

    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                           LeaveTypeRepository leaveTypeRepository, LeaveRequestRepository leaveRequestRepository,
                           AttendanceRepository attendanceRepository, HolidayRepository holidayRepository,
                           ScheduleRepository scheduleRepository, PaymentRepository paymentRepository,
                           SystemLogRepository systemLogRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidayRepository = holidayRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
        this.systemLogRepository = systemLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Αν υπάρχουν ήδη ρόλοι, σταματάμε για να μην κάνουμε διπλότυπα
        if (roleRepository.count() > 0) {
            System.out.println("⚠️ Data already initialized. Skipping...");
            return;
        }

        System.out.println("🌱 Initializing FULL Database Data...");

        // ==========================================
        // 1. ΒΑΣΙΚΑ DEDOMENA (Roles, Depts, Holidays)
        // ==========================================

        // Roles
        Role adminRole = new Role(); adminRole.setName("ADMIN"); adminRole.setDescription("System Administrator");
        Role managerRole = new Role(); managerRole.setName("MANAGER"); managerRole.setDescription("Department Manager");
        Role employeeRole = new Role(); employeeRole.setName("EMPLOYEE"); employeeRole.setDescription("Regular Employee");
        List<Role> roles = roleRepository.saveAll(Arrays.asList(adminRole, managerRole, employeeRole));
        adminRole = roles.get(0);
        employeeRole = roles.get(2);

        // Departments
        Department itDept = new Department(); itDept.setName("IT"); itDept.setDescription("Development & Support");
        Department hrDept = new Department(); hrDept.setName("HR"); hrDept.setDescription("Human Resources");
        Department salesDept = new Department(); salesDept.setName("Sales"); salesDept.setDescription("Sales & Marketing");
        Department financeDept = new Department(); financeDept.setName("Finance"); financeDept.setDescription("Accounting");
        List<Department> depts = departmentRepository.saveAll(Arrays.asList(itDept, hrDept, salesDept, financeDept));
        itDept = depts.get(0);
        salesDept = depts.get(2);

        // Holidays (Τρέχον έτος)
        int year = LocalDate.now().getYear();
        createHoliday("New Year's Day", LocalDate.of(year, 1, 1));
        createHoliday("Epiphany", LocalDate.of(year, 1, 6));
        createHoliday("Independence Day", LocalDate.of(year, 3, 25));
        createHoliday("Labor Day", LocalDate.of(year, 5, 1));
        createHoliday("Ochi Day", LocalDate.of(year, 10, 28));
        createHoliday("Christmas", LocalDate.of(year, 12, 25));

        // Leave Types
        LeaveType sickLeave = new LeaveType(); sickLeave.setName("Sick Leave"); sickLeave.setMaxDays(10);
        LeaveType annualLeave = new LeaveType(); annualLeave.setName("Annual Leave"); annualLeave.setMaxDays(25);
        List<LeaveType> leaveTypes = leaveTypeRepository.saveAll(Arrays.asList(sickLeave, annualLeave));
        annualLeave = leaveTypes.get(1);

        // ==========================================
        // 2. ΧΡΗΣΤΕΣ & ΥΠΑΛΛΗΛΟΙ
        // ==========================================

        // Admin User (Χωρίς Employee profile)
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("1234"));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);
        createLog(adminUser, "LOGIN", "Admin logged in via system init");

        System.out.println("✅ Admin created: username='admin', password='1234'");

        // Τυχαίοι Υπάλληλοι
        Random rand = new Random();
        String[] firstNames = {"Giorgos", "Maria", "Nikos", "Eleni", "Dimitris", "Katerina", "Giannis", "Sofia", "Kostas", "Anna", "Alex", "Vaso"};
        String[] lastNames = {"Papadopoulos", "Georgiou", "Nikolaou", "Dimitriou", "Andreou", "Christou", "Makris", "Raptis", "Sotiropoulos", "Vlachos"};

        for (int i = 0; i < 15; i++) {
            String fName = firstNames[rand.nextInt(firstNames.length)];
            String lName = lastNames[rand.nextInt(lastNames.length)];
            String username = fName.toLowerCase() + "." + lName.toLowerCase() + i;

            // User
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("1234"));
            user.setRole(employeeRole);

            // Employee
            Employee emp = new Employee();
            emp.setFirstName(fName);
            emp.setLastName(lName);
            emp.setEmail(username + "@company.com");
            emp.setPhone("69" + (10000000 + rand.nextInt(89999999)));
            emp.setAddress("Street " + rand.nextInt(100) + ", Athens");
            emp.setHireDate(LocalDate.now().minusMonths(rand.nextInt(36))); // 0-3 χρόνια
            emp.setSalary(900.0 + rand.nextInt(2000));
            emp.setDepartment(depts.get(rand.nextInt(depts.size())));

            emp.setUser(user); // Σύνδεση
            employeeRepository.save(emp); // Αποθήκευση (Cascades to User)

            // --- EXTRAS ΓΙΑ ΚΑΘΕ ΥΠΑΛΛΗΛΟ ---

            // A. Παρουσίες (Attendance) - Τελευταίες 7 μέρες
            for (int d = 0; d < 7; d++) {
                LocalDate date = LocalDate.now().minusDays(d);
                if (date.getDayOfWeek().getValue() >= 6) continue; // Skip Weekend

                if (rand.nextInt(10) > 1) { // 90% πιθανότητα παρουσίας
                    Attendance att = new Attendance();
                    att.setEmployee(emp);
                    att.setDate(date);
                    att.setCheckInTime(LocalTime.of(8, rand.nextInt(30)));
                    att.setCheckOutTime(LocalTime.of(16, rand.nextInt(59)));
                    attendanceRepository.save(att);
                }
            }

            // B. Πρόγραμμα (Schedules) - Επόμενη εβδομάδα
            for (int d = 1; d <= 5; d++) { // Δευ-Παρ
                Schedule sch = new Schedule();
                sch.setEmployee(emp);
                sch.setDate(LocalDate.now().plusDays(d));
                sch.setStartTime(LocalTime.of(9, 0));
                sch.setEndTime(LocalTime.of(17, 0));
                sch.setShiftName("Morning Shift");
                scheduleRepository.save(sch);
            }

            // C. Μισθοδοσία (Payments) - Προηγούμενος μήνας
            Payment pay = new Payment();
            pay.setEmployee(emp);
            pay.setAmount(emp.getSalary());
            pay.setPaymentDate(LocalDate.now().minusDays(15)); // Πληρώθηκε πριν 15 μέρες
            pay.setMonthYear(YearMonth.now().minusMonths(1).toString()); // π.χ. "2023-11"
            paymentRepository.save(pay);

            // D. Άδειες (Leave Requests)
            if (rand.nextBoolean()) {
                LeaveRequest lr = new LeaveRequest();
                lr.setEmployee(emp);
                lr.setLeaveType(annualLeave);
                lr.setStartDate(LocalDate.now().plusDays(rand.nextInt(60)));
                lr.setEndDate(lr.getStartDate().plusDays(2));
                lr.setReason("Vacation");

                // Random Enum Status
                int s = rand.nextInt(3);
                lr.setStatus(s == 0 ? LeaveStatus.APPROVED : (s == 1 ? LeaveStatus.PENDING : LeaveStatus.REJECTED));

                leaveRequestRepository.save(lr);
            }

            // E. Logs
            createLog(user, "LOGIN", "User logged in");
        }

        System.out.println("✅ Full Data Initialization Completed!");
    }

    // Helper για Αργίες
    private void createHoliday(String name, LocalDate date) {
        Holiday h = new Holiday();
        h.setName(name);
        h.setDate(date);
        holidayRepository.save(h);
    }

    // Helper για Logs
    private void createLog(User user, String action, String details) {
        SystemLog log = new SystemLog();
        log.setUser(user);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now().minusHours(new Random().nextInt(48)));
        // Αν υπάρχει πεδίο details/description, βάλτο εδώ. Αν όχι, αγνόησέ το.
        systemLogRepository.save(log);
    }
}