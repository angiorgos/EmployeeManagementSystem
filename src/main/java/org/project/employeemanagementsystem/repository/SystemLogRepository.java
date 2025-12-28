package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    // Βρίσκει τι ενέργειες έκανε ένας συγκεκριμένος χρήστης
    List<SystemLog> findByUserId(Long userId);
}