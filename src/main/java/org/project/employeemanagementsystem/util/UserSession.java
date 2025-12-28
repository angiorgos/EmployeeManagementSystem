package org.project.employeemanagementsystem.util;

import lombok.Data;
import org.project.employeemanagementsystem.model.User;
import org.springframework.stereotype.Component;

@Component
@Data
public class UserSession {

    private User currentUser;

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

}