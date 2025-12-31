package org.project.employeemanagementsystem;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final ScheduleRepository scheduleRepository;
    private final SystemLogRepository systemLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository,
                      DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                      AttendanceRepository attendanceRepository, HolidayRepository holidayRepository,
                      LeaveTypeRepository leaveTypeRepository, ScheduleRepository scheduleRepository,
                      SystemLogRepository systemLogRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidayRepository = holidayRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.scheduleRepository = scheduleRepository;
        this.systemLogRepository = systemLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            System.out.println("--- 🚀 STARTING DATABASE SEEDING ---");

            // 1. ROLES
            Role adminRole = new Role(); adminRole.setName("ADMIN"); adminRole.setDescription("Admin");
            Role userRole = new Role(); userRole.setName("USER"); userRole.setDescription("Employee");
            roleRepository.saveAll(Arrays.asList(adminRole, userRole));

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setRole(adminRole);
            userRepository.save(admin);

            // 2. DEPARTMENTS (Χρήση Setters για να μην χτυπάει ο Constructor)
            Department it = new Department(); it.setName("IT"); it.setDescription("Tech");
            departmentRepository.save(it);
            Department hr = new Department(); hr.setName("HR"); hr.setDescription("Human Res");
            departmentRepository.save(hr);
            Department sales = new Department(); sales.setName("Sales"); sales.setDescription("Sales");
            departmentRepository.save(sales);

            // 3. HOLIDAYS
            createHoliday("New Year", LocalDate.of(LocalDate.now().getYear(), 1, 1));
            createLeaveType("Annual", 20);
            createLeaveType("Sick", 10);

            // 4. EMPLOYEES (Χωρίς setSchedule εδώ)
            Employee emp1 = createEmployee("Georgios", "Papadopoulos", "g.pap@company.com", "6971111111", "100", 1800.0, it);
            Employee emp2 = createEmployee("Maria", "Dimitriou", "m.dim@company.com", "6972222222", "200", 1400.0, hr);

            createUserForEmployee("gpapadopoulos", userRole);

            // 5. SCHEDULES
            createScheduleForWeek(emp1, "Morning", LocalTime.of(9, 0), LocalTime.of(17, 0));
            createScheduleForWeek(emp2, "Morning", LocalTime.of(9, 0), LocalTime.of(17, 0));

            // 6. LOGS
            createLog("admin", "System initialized");

            System.out.println("✅ Database Seeded Successfully!");
        }
    }

    private Employee createEmployee(String f, String l, String e, String p, String ssn, Double sal, Department d) {
        Employee emp = new Employee();
        emp.setFirstName(f); emp.setLastName(l); emp.setEmail(e); emp.setPhone(p);
        emp.setSsn(ssn); emp.setSalary(sal); emp.setDepartment(d); emp.setAddress("GR");
        emp.setHireDate(LocalDate.now().minusYears(1));
        return employeeRepository.save(emp);
    }

    private void createScheduleForWeek(Employee emp, String name, LocalTime start, LocalTime end) {
        LocalDate date = LocalDate.now();
        for (int i=0; i<5; i++) {
            if (date.getDayOfWeek().getValue() <= 5) {
                Schedule s = new Schedule();
                s.setEmployee(emp);
                s.setDate(date);
                s.setShiftName(name);
                s.setStartTime(start);
                s.setEndTime(end);
                scheduleRepository.save(s);
            }
            date = date.plusDays(1);
        }
    }

    private void createUserForEmployee(String u, Role r) {
        User user = new User(); user.setUsername(u); user.setPassword(passwordEncoder.encode("1234")); user.setRole(r);
        userRepository.save(user);
    }

    private void createHoliday(String n, LocalDate d) { Holiday h = new Holiday(); h.setName(n); h.setDate(d); holidayRepository.save(h); }
    private void createLeaveType(String n, int d) { LeaveType l = new LeaveType(); l.setName(n); l.setMaxDays(d); leaveTypeRepository.save(l); }

    private void createLog(String u, String a) {
        SystemLog log = new SystemLog();
        log.setUsername(u);
        log.setAction(a);
        log.setTimestamp(LocalDateTime.now());
        systemLogRepository.save(log);
    }
}