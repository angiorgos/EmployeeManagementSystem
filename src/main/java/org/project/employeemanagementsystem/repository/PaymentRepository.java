package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Μπορούμε να βρούμε πληρωμές με βάση τον μήνα (π.χ. "12/2024")
    List<Payment> findByMonthYear(String monthYear);
}