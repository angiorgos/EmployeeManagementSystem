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
                                        double otRate, double sundayRate,
                                        double totalTaxRate, double employerSplitPct) { // Νέες παράμετροι

        double standardHours = 176.0;
        double salary = (emp.getSalary() != null) ? emp.getSalary() : 0.0;

        double hourlyPay = salary / standardHours;
        double otPay = hourlyPay * otRate * otHours;
        double sunPay = hourlyPay * sundayRate * sundayHours;
        double penalty = (hoursWorked < standardHours) ? (standardHours - hoursWorked) * hourlyPay : 0;
        double bonus = 0.0;

        // 1. Υπολογισμός Μικτών
        double gross = salary - penalty + otPay + sunPay + bonus;

        // 2. Υπολογισμός Φόρων (Η λογική του φίλου σου, διορθωμένη)
        // Παράδειγμα: Gross 1000€, TaxRate 0.40 (400€ φόρος)
        // EmployerSplit 0.60 (Ο εργοδότης πληρώνει το 60% του φόρου)

        double totalTaxValue = gross * totalTaxRate; // 400€

        double employerShare = totalTaxValue * employerSplitPct; // 400 * 0.60 = 240€ (Επιβάρυνση Εργοδότη)
        double employeeShare = totalTaxValue - employerShare;    // 400 - 240 = 160€ (Κράτηση Υπαλλήλου)

        // 3. Καθαρά (Μικτά - Μερίδιο Υπαλλήλου)
        double netAmount = gross - employeeShare;

        Payment payment = new Payment();
        payment.setEmployee(emp);
        payment.setPaymentDate(LocalDate.now());
        payment.setMonthYear(LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));

        payment.setBaseSalary(salary);
        payment.setGrossPay(round(gross));

        // Αποθήκευση των μεριδίων
        payment.setDeductions(round(employeeShare)); // Αυτά αφαιρούνται από τον υπάλληλο
        payment.setEmployerTax(round(employerShare)); // Αυτά τα πληρώνει η εταιρεία εξτρά

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