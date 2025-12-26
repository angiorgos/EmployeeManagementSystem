package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom μέθοδος DAO (αντί για SQL: SELECT * FROM users WHERE username = ?)
    Optional<User> findByUsername(String username);
}