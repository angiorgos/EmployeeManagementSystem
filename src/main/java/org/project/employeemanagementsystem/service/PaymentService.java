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

    // --- ΔΙΟΡΘΩΣΗ: Προστέθηκε η παράμετρος 'payrollDate' ---
    public void calculateAndSavePayroll(Employee emp, LocalDate payrollDate,
                                        Double hoursWorked, Double overtimeHours, Double sundayHours,
                                        double overtimeRate, double sundayRate, double totalTaxRate, double employerShare) {

        // Χρησιμοποιούμε την ημερομηνία που επιλέχθηκε (payrollDate), ΟΧΙ την τωρινή (now)
        String currentMonth = payrollDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));

        // Έλεγχος: Αν υπάρχει ήδη πληρωμή για αυτόν τον υπάλληλο και αυτόν τον μήνα, ίσως να μην θες να την ξαναφτιάξεις.
        // Για την ώρα το αφήνουμε απλό (δημιουργεί νέα εγγραφή).

        double hourlyRate = emp.getSalary() / 160.0;

        double basePay = emp.getSalary();
        double overtimePay = overtimeHours * hourlyRate * overtimeRate;
        double sundayPay = sundayHours * hourlyRate * sundayRate;

        double grossPay = basePay + overtimePay + sundayPay;

        double totalTaxAmount = grossPay * totalTaxRate;
        double employerTaxAmount = totalTaxAmount * employerShare;
        double employeeTaxAmount = totalTaxAmount * (1 - employerShare);

        Payment payment = new Payment();
        payment.setEmployee(emp);
        payment.setMonthYear(currentMonth); // "02/2026" π.χ.
        payment.setPaymentDate(payrollDate); // Η ημερομηνία που επέλεξες
        payment.setBaseSalary(basePay);
        payment.setHoursWorked(hoursWorked);
        payment.setOvertimeHours(overtimeHours);
        payment.setSundayHours(sundayHours);

        payment.setGrossPay(grossPay);
        payment.setDeductions(employeeTaxAmount);
        payment.setEmployerTax(employerTaxAmount);

        payment.setAmount(grossPay - employeeTaxAmount);
        payment.setStatus("PENDING");

        paymentRepository.save(payment);
    }

    public void updateBonus(Payment payment, double bonusAmount, double employeeTaxRate) {
        payment.setBonus(bonusAmount);

        // Προσθήκη Bonus στο Gross Pay
        // Σημείωση: Εδώ κάνουμε μια απλοποίηση. Αν ήθελες τέλεια ακρίβεια θα έπρεπε να ξανα-υπολογίσεις τα πάντα.
        // Αλλά για το Bonus, προσθέτουμε απλά το ποσό.
        double oldGross = payment.getGrossPay();
        double newGross = oldGross + bonusAmount;

        double bonusTax = bonusAmount * employeeTaxRate;

        double oldDeductions = payment.getDeductions();
        payment.setDeductions(oldDeductions + bonusTax);

        payment.setGrossPay(newGross);
        payment.setAmount(newGross - payment.getDeductions());

        paymentRepository.save(payment);
    }

    public void updatePaymentStatus(Payment payment, String status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }
}