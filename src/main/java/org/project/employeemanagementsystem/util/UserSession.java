package org.project.employeemanagementsystem.util;

import org.project.employeemanagementsystem.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserSession {

    private User currentUser;

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}