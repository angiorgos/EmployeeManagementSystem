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

    private static final String KEY_WORK_HOURS = "payroll.standard_hours";
    private static final String KEY_OVERTIME = "payroll.overtime_rate";
    private static final String KEY_SUNDAY = "payroll.sunday_rate";

    // Ποσοστό Εισφορών Εργαζόμενου
    private static final String KEY_SOCIAL_RATE = "payroll.social_rate";
    // Ποσοστό Φόρου Εισοδήματος
    private static final String KEY_INCOME_TAX_RATE = "payroll.income_tax_rate";
    // Ποσοστό Εισφορών Εργοδότη
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


    @Transactional
    public void generateMonthlyPayroll(LocalDate selectedDate) {

        double stdHours = settingService.getDouble(KEY_WORK_HOURS, 176.0);
        double otRate   = settingService.getDouble(KEY_OVERTIME, 1.50);
        double sunRate  = settingService.getDouble(KEY_SUNDAY, 1.75);


        double socialRate = settingService.getDouble(KEY_SOCIAL_RATE, 0.14);      // 14% Εισφορές
        double taxRate    = settingService.getDouble(KEY_INCOME_TAX_RATE, 0.10);  // 10% Φόρος
        double emplRate   = settingService.getDouble(KEY_EMPLOYER_RATE, 0.22);    // 22% Εργοδοτικές

        String monthStr = selectedDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        List<Employee> employees = employeeService.getAllEmployees();
        int count = 0;

        for (Employee emp : employees) {
            if (emp.getSalary() == null) continue;
            if (emp.getExitDate() != null && emp.getExitDate().isBefore(selectedDate.withDayOfMonth(1))) continue;

            //Υπολογισμός Μικτών
            double realHours = attendanceService.calculateTotalHoursWorked(emp, selectedDate);
            double sundayHours = attendanceService.calculateSundayHours(emp, selectedDate);
            double overtimeHours = Math.max(0, realHours - stdHours);

            double hourlyRate = (stdHours > 0) ? emp.getSalary() / stdHours : 0;
            double otPay = overtimeHours * hourlyRate * otRate;
            double sunPay = sundayHours * hourlyRate * sunRate;

            double grossPay = emp.getSalary() + otPay + sunPay;



            // Εισφορές Εργαζόμενου (Επί του Μικτού)
            double employeeDeductions = grossPay * socialRate;

            // Φορολογητέο Εισόδημα (Μικτά - Εισφορές)
            double taxableIncome = grossPay - employeeDeductions;

            // Φόρος (Επί του Φορολογητέου)
            double incomeTax = taxableIncome * taxRate;

            // Καθαρά (Μικτά - Εισφορές - Φόρος)
            double netPay = grossPay - employeeDeductions - incomeTax;

            //Κόστος Εργοδότη (Επί του Μικτού, δεν αφαιρείται από τον υπάλληλο)
            double employerCost = grossPay * emplRate;

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
            p.setDeductions(round(employeeDeductions));
            p.setTotalTax(round(incomeTax));
            p.setEmployerTax(round(employerCost));
            p.setAmount(round(netPay));

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

        double oldBonus = (payment.getBonus() != null) ? payment.getBonus() : 0.0;
        double workPay = payment.getGrossPay() - oldBonus;

        double newGross = workPay + newBonus;

        double employeeDeductions = newGross * socialRate;
        double taxableIncome = newGross - employeeDeductions;
        double incomeTax = taxableIncome * taxRate;
        double employerCost = newGross * emplRate;
        double netPay = newGross - employeeDeductions - incomeTax;

        payment.setBonus(newBonus);
        payment.setGrossPay(round(newGross));
        payment.setDeductions(round(employeeDeductions));
        payment.setTotalTax(round(incomeTax));
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