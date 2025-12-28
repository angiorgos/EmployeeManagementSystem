package org.project.employeemanagementsystem.service;


import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;

     public AuthService(UserRepository userRepository) {
         this.userRepository = userRepository;
     }


    public User login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        if (!user.getPassword().equals(password)) {
            return null;
        }
        if (!user.isActive()) {
            return null;
        }
        return user;
    }
}
