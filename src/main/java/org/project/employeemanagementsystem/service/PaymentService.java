package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SystemLogService systemLogService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final SystemSettingService settingService;

    // --- ΝΕΑ KEYS ΓΙΑ ΡΕΑΛΙΣΤΙΚΟ ΥΠΟΛΟΓΙΣΜΟ ---
    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";

    // Ποσοστό Εισφορών Εργαζόμενου (π.χ. 0.14)
    private static final String KEY_SOCIAL_RATE = "payroll.social_rate";
    // Ποσοστό Φόρου Εισοδήματος (π.χ. 0.10)
    private static final String KEY_INCOME_TAX_RATE = "payroll.income_tax_rate";
    // Ποσοστό Εισφορών Εργοδότη (π.χ. 0.22)
    private static final String KEY_EMPLOYER_RATE = "payroll.employer_rate";

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          SystemLogService systemLogService,
                          EmployeeService employeeService,
                          AttendanceService attendanceService,
                          SystemSettingService settingService) {
        this.paymentRepository = paymentRepository;
        this.systemLogService = systemLogService;
        this.employeeService = employeeService;
        this.attendanceService = attendanceService;
        this.settingService = settingService;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Δημιουργία Μισθοδοσίας με Ρεαλιστικό Υπολογισμό
     */
    @Transactional
    public void generateMonthlyPayroll(LocalDate selectedDate) {
        // Λήψη Ρυθμίσεων
        double stdHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double otRate   = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate  = settingService.getDouble(KEY_SUNDAY, 1.75);

        // Οι νέοι συντελεστές
        double socialRate = settingService.getDouble(KEY_SOCIAL_RATE, 0.14);      // 14% Εισφορές
        double taxRate    = settingService.getDouble(KEY_INCOME_TAX_RATE, 0.10);  // 10% Φόρος
        double emplRate   = settingService.getDouble(KEY_EMPLOYER_RATE, 0.22);    // 22% Εργοδοτικές

        String monthStr = selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;
            if (emp.getExitDate() != null && emp.getExitDate().isBefore(selectedDate.withDayOfMonth(1))) continue;

            // 1. Υπολογισμός Μικτών (Gross)
            double realHours = attendanceService.calculateTotalHoursWorked(emp, selectedDate);
            double sundayHours = attendanceService.calculateSundayHours(emp, selectedDate);
            double overtimeHours = Math.max(0, realHours - stdHours);

            double hourlyRate = (stdHours > 0) ? emp.getSalary() / stdHours : 0;
            double otPay = overtimeHours * hourlyRate * otRate;
            double sunPay = sundayHours * hourlyRate * sunRate;

            double grossPay = emp.getSalary() + otPay + sunPay;

            // --- Ο ΡΕΑΛΙΣΤΙΚΟΣ ΥΠΟΛΟΓΙΣΜΟΣ ---

            // Βήμα Α: Εισφορές Εργαζόμενου (Επί του Μικτού)
            double employeeDeductions = grossPay * socialRate;

            // Βήμα Β: Φορολογητέο Εισόδημα (Μικτά - Εισφορές)
            double taxableIncome = grossPay - employeeDeductions;

            // Βήμα Γ: Φόρος (Επί του Φορολογητέου)
            double incomeTax = taxableIncome * taxRate;

            // Βήμα Δ: Καθαρά (Μικτά - Εισφορές - Φόρος)
            double netPay = grossPay - employeeDeductions - incomeTax;

            // Βήμα Ε: Κόστος Εργοδότη (Επί του Μικτού, δεν αφαιρείται από τον υπάλληλο)
            double employerCost = grossPay * emplRate;

            // --- ΔΗΜΙΟΥΡΓΙΑ OBJECT ---
            Payment p = new Payment();
            p.setEmployee(emp);
            p.setMonthYear(monthStr);
            p.setPaymentDate(LocalDate.now());
            p.setBaseSalary(round(emp.getSalary()));
            p.setHoursWorked(stdHours);
            p.setOvertimeHours(round(overtimeHours));
            p.setSundayHours(round(sundayHours));
            p.setBonus(0.0);

            p.setGrossPay(round(grossPay));
            p.setDeductions(round(employeeDeductions)); // Εισφορές
            p.setTotalTax(round(incomeTax));            // Φόρος (Τώρα χρησιμοποιείται!)
            p.setEmployerTax(round(employerCost));      // Εργοδοτικές
            p.setAmount(round(netPay));                 // Καθαρά

            p.setStatus("PENDING");
            paymentRepository.save(p);
            count++;
        }
        systemLogService.log("BATCH_PAYROLL", "Generated realistic payroll for " + count + " employees.");
    }

    @Transactional
    public Payment updateBonus(Payment payment, double newBonus) {
        double socialRate = settingService.getDouble(KEY_SOCIAL_RATE, 0.14);
        double taxRate    = settingService.getDouble(KEY_INCOME_TAX_RATE, 0.10);
        double emplRate   = settingService.getDouble(KEY_EMPLOYER_RATE, 0.22);

        // Βρίσκουμε τον μισθό εργασίας (χωρίς το παλιό bonus)
        double oldBonus = (payment.getBonus() != null) ? payment.getBonus() : 0.0;
        double workPay = payment.getGrossPay() - oldBonus;

        // Νέα Μικτά
        double newGross = workPay + newBonus;

        // Επανυπολογισμός με τη ρεαλιστική μέθοδο
        double employeeDeductions = newGross * socialRate;
        double taxableIncome = newGross - employeeDeductions;
        double incomeTax = taxableIncome * taxRate;
        double employerCost = newGross * emplRate;
        double netPay = newGross - employeeDeductions - incomeTax;

        payment.setBonus(newBonus);
        payment.setGrossPay(round(newGross));
        payment.setDeductions(round(employeeDeductions));
        payment.setTotalTax(round(incomeTax)); // Ενημερώνουμε και τον φόρο
        payment.setEmployerTax(round(employerCost));
        payment.setAmount(round(netPay));

        return paymentRepository.save(payment);
    }

    @Transactional
    public void finalizePayment(Payment p) {
        p.setStatus("PAID");
        paymentRepository.save(p);
    }

    private double round(double val) { return Math.round(val * 100.0) / 100.0; }
}