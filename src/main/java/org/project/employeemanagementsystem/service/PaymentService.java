package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void calculateAndSavePayroll(Employee emp, double hoursWorked, double otHours, double sundayHours,
                                        double otRate, double sundayRate, double insuranceRate) {

        // 1. Βασικοί Υπολογισμοί
        double standardHours = 176.0; // Τυπικές ώρες μήνα
        double hourlyPay = emp.getSalary() / standardHours; // Ωρομίσθιο

        // 2. Υπολογισμός Προσαυξήσεων
        double otPay = hourlyPay * otRate * otHours;           // Πληρωμή υπερωριών
        double sunPay = hourlyPay * sundayRate * sundayHours;  // Πληρωμή Κυριακών

        // 3. Πέναλτι (αν δούλεψε λιγότερο από το κανονικό)
        double penalty = 0.0;
        if (hoursWorked < standardHours) {
            penalty = (standardHours - hoursWorked) * hourlyPay;
        }

        // 4. Μικτά (Gross)
        double gross = emp.getSalary() - penalty + otPay + sunPay;

        // 5. Κρατήσεις (Insurance)
        // Υποθέτουμε ότι το insuranceRate (π.χ. 0.30) είναι το συνολικό κόστος
        // και ο εργαζόμενος πληρώνει το μισό (ή όσο ορίσεις εσύ).
        double employeeShare = 0.5;
        double deductions = gross * insuranceRate * employeeShare;

        // 6. Καθαρά (Net Pay)
        double netAmount = gross - deductions;

        // --- ΔΗΜΙΟΥΡΓΙΑ ΚΑΙ ΑΠΟΘΗΚΕΥΣΗ TOY ENTITY ---
        Payment payment = new Payment();

        payment.setEmployee(emp);
        payment.setPaymentDate(LocalDate.now());
        payment.setMonthYear(LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));

        // Αποθήκευση Αναλυτικών Στοιχείων (New Fields)
        payment.setBaseSalary(emp.getSalary());
        payment.setHoursWorked(hoursWorked);
        payment.setOvertimeHours(otHours);
        payment.setSundayHours(sundayHours);
        payment.setGrossPay(round(gross));
        payment.setDeductions(round(deductions));
        payment.setAmount(round(netAmount)); // Το τελικό ποσό

        payment.setStatus("PENDING");

        paymentRepository.save(payment);
    }

    // Βοηθητική μέθοδος για στρογγυλοποίηση σε 2 δεκαδικά
    private Double round(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}