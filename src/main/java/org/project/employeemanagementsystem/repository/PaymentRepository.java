package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Βρίσκει όλο το ιστορικό πληρωμών ενός υπαλλήλου
    List<Payment> findByEmployeeId(Long employeeId);
}