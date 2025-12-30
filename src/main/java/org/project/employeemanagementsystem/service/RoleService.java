package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.Role;
import org.project.employeemanagementsystem.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role findByName(String name) {
        return roleRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Role '" + name + "' not found!"));
    }

    public void saveRole(Role role) {
        roleRepository.save(role);
    }
}