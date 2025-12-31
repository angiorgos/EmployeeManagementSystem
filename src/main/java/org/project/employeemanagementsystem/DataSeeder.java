package org.project.employeemanagementsystem ;

import org.project.employeemanagementsystem.model.*;
import org.project.employeemanagementsystem.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, RoleRepository roleRepository,
                      DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Έλεγχος αν υπάρχουν ήδη δεδομένα
        if (roleRepository.count() > 0) return;

        // 1. Δημιουργία Ρόλων
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);

        // 2. Δημιουργία Τμήματος (Department)
        Department it = new Department();
        it.setName("IT Department");
        departmentRepository.save(it);

        // 3. Δημιουργία User (Login credentials)
        User adminUser = new User();
        adminUser.setUsername("a");
        adminUser.setPassword(passwordEncoder.encode("1"));
        adminUser.setRole(adminRole);
        // Προσοχή: Δεν ορίζουμε το employee εδώ, γιατί το User έχει 'mappedBy'.
        // Η σχέση ελέγχεται από την πλευρά του Employee.
        userRepository.save(adminUser);

        // 4. Δημιουργία Employee (Προσωπικά στοιχεία)
        Employee adminEmployee = new Employee();
        adminEmployee.setFirstName("System");
        adminEmployee.setLastName("Admin");
        adminEmployee.setEmail("admin@ems.com");

        // ΥΠΟΧΡΕΩΤΙΚΑ ΠΕΔΙΑ (βάσει του κώδικα που έστειλες)
        adminEmployee.setPhone("6900000000"); // Unique & Not Null
        adminEmployee.setAddress("Headquarters, Room 101");
        adminEmployee.setSsn("AA000000");     // Unique
        adminEmployee.setHireDate(LocalDate.now()); // Not Null
        adminEmployee.setSalary(5000.0);

        // Σύνδεση με Τμήμα
        adminEmployee.setDepartment(it);

        // 5. ΣΥΝΔΕΣΗ: Εδώ "παντρεύουμε" τον Employee με τον User
        // Επειδή στο Employee entity έχεις @JoinColumn(name = "user_id"),
        // αυτός είναι ο ιδιοκτήτης της σχέσης.
        adminEmployee.setUser(adminUser);

        // Το @OneToOne(cascade = CascadeType.ALL) στο Employee θα φρόντιζε να σωθεί
        // και ο User αν δεν τον είχαμε σώσει, αλλά καλό είναι να τα σώζουμε με σειρά.
        employeeRepository.save(adminEmployee);

        System.out.println("---------------------------------------------");
        System.out.println("ADMIN CREATED SUCCESSFULLY");
        System.out.println("Username: a");
        System.out.println("Password: 1");
        System.out.println("Linked to Employee: System Admin (IT Department)");
        System.out.println("---------------------------------------------");
    }
}