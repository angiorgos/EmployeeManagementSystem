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
    private final SystemLogService systemLogService; // <--- Προσθήκη Audit

    // Constructor Injection
    @Autowired
    public PaymentService(PaymentRepository paymentRepository, SystemLogService systemLogService) {
        this.paymentRepository = paymentRepository;
        this.systemLogService = systemLogService;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Υπολογισμός Αρχικής Μισθοδοσίας
     */
    @Transactional
    public void calculateAndSavePayroll(Employee emp, LocalDate payrollDate,
                                        Double standardMonthlyHours,
                                        Double overtimeHours, Double sundayHours,
                                        double overtimeRate, double sundayRate,
                                        double totalTaxRate, double employerShare) {

        String currentMonth = payrollDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));

        // 1. Υπολογισμοί
        double divisor = (standardMonthlyHours != null && standardMonthlyHours > 0) ? standardMonthlyHours : 173.33;
        double hourlyRate = emp.getSalary() / divisor;

        double basePay = emp.getSalary();
        double overtimePay = overtimeHours * hourlyRate * overtimeRate;
        double sundayPay = sundayHours * hourlyRate * sundayRate;

        double grossPay = basePay + overtimePay + sundayPay;

        double totalTaxAmount = grossPay * totalTaxRate;
        double employerTaxAmount = totalTaxAmount * employerShare;
        double employeeTaxAmount = totalTaxAmount * (1 - employerShare);

        // 2. Δημιουργία Αντικειμένου
        Payment payment = new Payment();
        payment.setEmployee(emp);
        payment.setMonthYear(currentMonth);
        payment.setPaymentDate(payrollDate);

        payment.setBaseSalary(basePay);
        payment.setHoursWorked(standardMonthlyHours);
        payment.setOvertimeHours(overtimeHours);
        payment.setSundayHours(sundayHours);
        payment.setBonus(0.0);

        payment.setGrossPay(grossPay);
        payment.setEmployerTax(employerTaxAmount);
        payment.setDeductions(employeeTaxAmount);

        payment.setAmount(grossPay - employeeTaxAmount);
        payment.setStatus("PENDING");

        paymentRepository.save(payment);

        // --- AUDIT LOG ---
        String details = String.format("Payroll Generated: %s %s | Month: %s | Net: %.2f €",
                emp.getFirstName(), emp.getLastName(), currentMonth, payment.getAmount());

        systemLogService.log("GENERATE_PAYROLL", details);
    }

    /**
     * Ενημέρωση Bonus (Προσθήκη στο Gross -> Επανυπολογισμός Φόρων -> Νέο Καθαρό)
     */
    @Transactional
    public Payment updateBonus(Payment payment, double newBonus, double totalTaxRate, double employerShare) {

        // 1. Βρίσκουμε τον μισθό εργασίας αφαιρώντας το παλιό bonus
        double oldBonus = (payment.getBonus() != null) ? payment.getBonus() : 0.0;
        double payFromWork = payment.getGrossPay() - oldBonus;

        // 2. Υπολογίζουμε το ΝΕΟ Gross (Μεικτά)
        double newGross = payFromWork + newBonus;

        // 3. Υπολογίζουμε ξανά τους φόρους στο ΝΕΟ Gross
        double totalTaxAmount = newGross * totalTaxRate;
        double employerTaxAmount = totalTaxAmount * employerShare;
        double employeeTaxAmount = totalTaxAmount * (1 - employerShare);

        // 4. Ενημερώνουμε την εγγραφή
        payment.setBonus(newBonus);
        payment.setGrossPay(newGross);
        payment.setEmployerTax(employerTaxAmount);
        payment.setDeductions(employeeTaxAmount);

        // 5. Νέο Καθαρό Πληρωτέο
        payment.setAmount(newGross - employeeTaxAmount);

        Payment savedPayment = paymentRepository.save(payment);

        // --- AUDIT LOG ---
        String details = String.format("Bonus Updated: %s %s | Old: %.2f -> New: %.2f | New Net: %.2f €",
                payment.getEmployee().getFirstName(), payment.getEmployee().getLastName(),
                oldBonus, newBonus, savedPayment.getAmount());

        systemLogService.log("UPDATE_BONUS", details);

        return savedPayment;
    }

    @Transactional
    public void updatePaymentStatus(Payment payment, String status) {
        String oldStatus = payment.getStatus();
        payment.setStatus(status);
        paymentRepository.save(payment);

        // --- AUDIT LOG ---
        String details = String.format("Status Changed: %s %s | Month: %s | %s -> %s",
                payment.getEmployee().getFirstName(), payment.getEmployee().getLastName(),
                payment.getMonthYear(), oldStatus, status);

        systemLogService.log("UPDATE_PAYMENT_STATUS", details);
    }
}