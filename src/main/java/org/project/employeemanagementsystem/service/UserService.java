package org.project.employeemanagementsystem.service;

import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Dependency Injection του DAO
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(User user) {
        // Εδώ θα μπορούσες να βάλεις έλεγχο, π.χ. αν το username υπάρχει ήδη
        return userRepository.save(user);
    }

    public boolean authenticate(String username, String password) {
        // Λογική ελέγχου κωδικού
        return userRepository.findByUsername(username)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}