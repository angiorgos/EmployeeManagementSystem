package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Βρίσκει τον χρήστη για το Login
    Optional<User> findByUsername(String username);

}