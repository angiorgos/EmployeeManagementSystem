package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    // Επιστρέφει τα logs ταξινομημένα από το πιο πρόσφατο στο παλαιότερο
    // Υποθέτουμε ότι το πεδίο στην κλάση SystemLog λέγεται 'timestamp'
    List<SystemLog> findAllByOrderByTimestampDesc();
}