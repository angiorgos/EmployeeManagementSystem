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

        double standardHours = 176.0;
        double hourlyPay = emp.getSalary() / standardHours;
        double otPay = hourlyPay * otRate * otHours;
        double sunPay = hourlyPay * sundayRate * sundayHours;
        double penalty = (hoursWorked < standardHours) ? (standardHours - hoursWorked) * hourlyPay : 0;

        double bonus = 0.0;

        // Υπολογισμός Μικτών
        double gross = emp.getSalary() - penalty + otPay + sunPay + bonus;

        // Υπολογισμός Κρατήσεων & Καθαρών
        double deductions = gross * insuranceRate * 0.5;
        double netAmount = gross - deductions;

        // ΔΗΜΙΟΥΡΓΙΑ ANTIKEIMENOY
        Payment payment = new Payment();


        payment.setEmployee(emp);

        payment.setPaymentDate(LocalDate.now());
        payment.setMonthYear(LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));

        payment.setBaseSalary(emp.getSalary());
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

    @Transactional
    public void updateBonus(Payment payment, double newBonus, double insuranceRate) {
        // Κρατάμε τα παλιά στοιχεία που δεν αλλάζουν (μισθός, ώρες)
        // και ξανακάνουμε τα μαθηματικά ΜΟΝΟ για τα λεφτά.

        // Ανάκτηση των ήδη υπολογισμένων ποσών από υπερωρίες (για να μην τα ξαναψάχνουμε)
        // Προσοχή: Εδώ κάνουμε reverse engineering ή τα ξαναυπολογίζουμε.
        // Για απλότητα: Gross = Net + Deductions.
        // Αλλά το σωστό είναι: Gross = Base + OT + Bonus.

        // Αφαιρούμε το παλιό bonus από τα μικτά και προσθέτουμε το καινούργιο
        double oldBonus = (payment.getBonus() != null) ? payment.getBonus() : 0.0;
        double currentGrossNoBonus = payment.getGrossPay() - oldBonus;

        double newGross = currentGrossNoBonus + newBonus;
        double newDeductions = newGross * insuranceRate * 0.5; // Ξαναβγάζουμε κρατήσεις
        double newNet = newGross - newDeductions;

        // Ενημέρωση Entity
        payment.setBonus(newBonus);
        payment.setGrossPay(round(newGross));
        payment.setDeductions(round(newDeductions));
        payment.setAmount(round(newNet));

        paymentRepository.save(payment);
    }

    // Βοηθητική μέθοδος για στρογγυλοποίηση σε 2 δεκαδικά
    private Double round(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}