package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        // Σταθερές
        double standardHours = 176.0; // Τυπικό 8ωρο x 22 μέρες

        // Αν ο μισθός είναι null, θεωρούμε 0 για να μην σκάσει
        double salary = (emp.getSalary() != null) ? emp.getSalary() : 0.0;

        double hourlyPay = salary / standardHours;
        double otPay = hourlyPay * otRate * otHours;
        double sunPay = hourlyPay * sundayRate * sundayHours;

        // Πέναλτι αν δούλεψε λιγότερο από το κανονικό (χωρίς άδεια)
        double penalty = (hoursWorked < standardHours) ? (standardHours - hoursWorked) * hourlyPay : 0;

        double bonus = 0.0; // Αρχικό bonus: 0

        // Υπολογισμός Μικτών
        double gross = salary - penalty + otPay + sunPay + bonus;

        // Υπολογισμός Κρατήσεων (Εργαζόμενου)
        double deductions = gross * insuranceRate * 0.5; // Π.χ. το μισό της εισφοράς

        // Υπολογισμός Καθαρών
        double netAmount = gross - deductions;

        // --- ΔΗΜΙΟΥΡΓΙΑ ΕΓΓΡΑΦΗΣ ---
        Payment payment = new Payment();

        // SOS: Αυτό έλειπε και πετούσε το DataIntegrityViolationException
        payment.setEmployee(emp);

        payment.setPaymentDate(LocalDate.now());
        payment.setMonthYear(LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));

        payment.setBaseSalary(salary);
        payment.setHoursWorked(hoursWorked);
        payment.setOvertimeHours(otHours);
        payment.setSundayHours(sundayHours);

        payment.setBonus(bonus);
        payment.setGrossPay(round(gross));
        payment.setDeductions(round(deductions));
        payment.setAmount(round(netAmount));

        payment.setStatus("PENDING");

        paymentRepository.save(payment);
    }

    // Μέθοδος για προσθήκη Bonus εκ των υστέρων
    @Transactional
    public void updateBonus(Payment payment, double newBonus, double insuranceRate) {
        double oldBonus = (payment.getBonus() != null) ? payment.getBonus() : 0.0;

        // Αφαιρούμε το παλιό bonus από τα μικτά για να βρούμε τη βάση
        double currentGrossNoBonus = payment.getGrossPay() - oldBonus;

        // Προσθέτουμε το νέο bonus
        double newGross = currentGrossNoBonus + newBonus;

        // Ξανα-υπολογίζουμε κρατήσεις και καθαρά
        double newDeductions = newGross * insuranceRate * 0.5;
        double newNet = newGross - newDeductions;

        payment.setBonus(newBonus);
        payment.setGrossPay(round(newGross));
        payment.setDeductions(round(newDeductions));
        payment.setAmount(round(newNet));

        paymentRepository.save(payment);
    }

    // Βοηθητική μέθοδος για στρογγυλοποίηση (2 δεκαδικά)
    private double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}