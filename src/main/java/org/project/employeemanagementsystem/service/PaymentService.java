package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Υπολογισμός Αρχικής Μισθοδοσίας
     */
    public void calculateAndSavePayroll(Employee emp, LocalDate payrollDate,
                                        Double standardMonthlyHours,
                                        Double overtimeHours, Double sundayHours,
                                        double overtimeRate, double sundayRate,
                                        double totalTaxRate, double employerShare) {

        String currentMonth = payrollDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));

        double divisor = (standardMonthlyHours != null && standardMonthlyHours > 0) ? standardMonthlyHours : 173.33;
        double hourlyRate = emp.getSalary() / divisor;

        double basePay = emp.getSalary();
        double overtimePay = overtimeHours * hourlyRate * overtimeRate;
        double sundayPay = sundayHours * hourlyRate * sundayRate;

        double grossPay = basePay + overtimePay + sundayPay;

        double totalTaxAmount = grossPay * totalTaxRate;
        double employerTaxAmount = totalTaxAmount * employerShare;
        double employeeTaxAmount = totalTaxAmount * (1 - employerShare);

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
    }

    /**
     * Ενημέρωση Bonus (Προσθήκη στο Gross -> Επανυπολογισμός Φόρων -> Νέο Καθαρό)
     * ΑΛΛΑΓΗ: Επιστρέφει πλέον Payment αντί για void.
     */
    public Payment updateBonus(Payment payment, double newBonus, double totalTaxRate, double employerShare) {

        // 1. Βρίσκουμε τον μισθό εργασίας αφαιρώντας το παλιό bonus (αν υπήρχε)
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

        // ΑΛΛΑΓΗ ΕΔΩ: Επιστρέφουμε το αντικείμενο που μόλις σώσαμε
        return paymentRepository.save(payment);
    }

    public void updatePaymentStatus(Payment payment, String status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }
}