package org.project.employeemanagementsystem.repository;

import org.project.employeemanagementsystem.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Βρίσκει ρόλο αγνοώντας πεζά/κεφαλαία
    Optional<Role> findByNameIgnoreCase(String name);
    Role findByName(String name);

}