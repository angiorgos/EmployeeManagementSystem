package org.project.employeemanagementsystem;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository,
                           DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                           LeaveTypeRepository leaveTypeRepository, LeaveRequestRepository leaveRequestRepository,
                           AttendanceRepository attendanceRepository, HolidayRepository holidayRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidayRepository = holidayRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Έλεγχος αν υπάρχουν ήδη δεδομένα
        if (roleRepository.count() > 0) {
            System.out.println("⚠️ Data already initialized. Skipping...");
            return;
        }

        System.out.println("🌱 Initializing Dummy Data...");

        // 1. Ρόλοι
        Role adminRole = new Role(); adminRole.setName("ADMIN"); adminRole.setDescription("System Administrator");
        Role managerRole = new Role(); managerRole.setName("MANAGER"); managerRole.setDescription("Department Manager");
        Role employeeRole = new Role(); employeeRole.setName("EMPLOYEE"); employeeRole.setDescription("Regular Employee");
        // Αποθηκεύουμε και κρατάμε τις ενημερωμένες αναφορές
        List<Role> roles = roleRepository.saveAll(Arrays.asList(adminRole, managerRole, employeeRole));
        adminRole = roles.get(0);
        managerRole = roles.get(1);
        employeeRole = roles.get(2);

        // 2. Τμήματα
        Department itDept = new Department(); itDept.setName("IT"); itDept.setDescription("Tech Department");
        Department hrDept = new Department(); hrDept.setName("HR"); hrDept.setDescription("Human Resources");
        Department salesDept = new Department(); salesDept.setName("Sales"); salesDept.setDescription("Sales Department");
        List<Department> depts = departmentRepository.saveAll(Arrays.asList(itDept, hrDept, salesDept));
        itDept = depts.get(0);
        hrDept = depts.get(1);
        salesDept = depts.get(2);

        // 3. Admin User (Standalone - χωρίς Employee profile)
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("1234"));
        adminUser.setRole(adminRole);
        userRepository.save(adminUser); // Εδώ χρειάζεται το save γιατί δεν υπάρχει Employee να το κάνει cascade
        System.out.println("✅ Admin created: username='admin', password='1234'");

        // 4. Τύποι Αδειών
        LeaveType sickLeave = new LeaveType(); sickLeave.setName("Sick Leave"); sickLeave.setMaxDays(10);
        LeaveType annualLeave = new LeaveType(); annualLeave.setName("Annual Leave"); annualLeave.setMaxDays(25);
        List<LeaveType> leaveTypes = leaveTypeRepository.saveAll(Arrays.asList(sickLeave, annualLeave));
        sickLeave = leaveTypes.get(0);
        annualLeave = leaveTypes.get(1);

        // 5. Δημιουργία Υπαλλήλων (Loop)
        Random rand = new Random();
        String[] names = {"Giorgos", "Maria", "Nikos", "Eleni", "Dimitris", "Katerina"};
        String[] surnames = {"Papadopoulos", "Georgiou", "Nikolaou", "Dimitriou", "Andreou", "Christou"};

        for (int i = 0; i < 15; i++) {
            String fName = names[rand.nextInt(names.length)];
            String lName = surnames[rand.nextInt(surnames.length)];
            String username = fName.toLowerCase() + "." + lName.toLowerCase() + i;

            // --- USER ---
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("1234"));
            user.setRole(employeeRole);
            // ❌ ΔΕΝ ΚΑΝΟΥΜΕ userRepository.save(user) ΕΔΩ!
            // Θα το κάνει αυτόματα το employeeRepository λόγω Cascade.

            // --- EMPLOYEE ---
            Employee emp = new Employee();
            emp.setFirstName(fName);
            emp.setLastName(lName);
            emp.setEmail(username + "@company.com");
            emp.setPhone("69" + rand.nextInt(99999999));
            emp.setAddress("Random St. " + rand.nextInt(100));
            emp.setHireDate(LocalDate.now().minusMonths(rand.nextInt(24)));
            emp.setSalary(1000.0 + rand.nextInt(1500));
            emp.setDepartment(rand.nextBoolean() ? itDept : salesDept);

            // Σύνδεση με τον User
            emp.setUser(user);

            // Αποθήκευση Employee (Αποθηκεύει ΚΑΙ τον User αυτόματα)
            employeeRepository.save(emp);

            // --- ATTENDANCE ---
            for (int d = 0; d < 5; d++) {
                Attendance att = new Attendance();
                att.setEmployee(emp);
                att.setDate(LocalDate.now().minusDays(d));
                att.setCheckInTime(LocalTime.of(9, 0));
                att.setCheckOutTime(LocalTime.of(17, 0));
                attendanceRepository.save(att);
            }

            // --- LEAVE REQUESTS ---
            if (rand.nextBoolean()) {
                LeaveRequest lr = new LeaveRequest();
                lr.setEmployee(emp);
                lr.setLeaveType(annualLeave);
                lr.setStartDate(LocalDate.now().plusDays(rand.nextInt(30)));
                lr.setEndDate(lr.getStartDate().plusDays(2));
                lr.setReason("Vacation");
                lr.setStatus(LeaveStatus.APPROVED);
                leaveRequestRepository.save(lr);
            }
        }

        System.out.println("✅ Data Initialization Completed!");
    }
}