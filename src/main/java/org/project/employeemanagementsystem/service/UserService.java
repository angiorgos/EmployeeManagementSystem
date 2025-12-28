package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(User user) {
        if(Objects.equals(user.getUsername(), user.getUsername())) {
            throw new IllegalArgumentException("Username already exists!");
        }
        return userRepository.save(user);
    }

    public boolean authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}