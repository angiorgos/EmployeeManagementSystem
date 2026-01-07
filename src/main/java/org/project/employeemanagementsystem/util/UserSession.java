package org.project.employeemanagementsystem.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.project.employeemanagementsystem.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserSession {

    //Το αποθηκεύουμε ως Property για να έχουμε Listeners
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();


    public User getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(User user) {
        this.currentUser.set(user);
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    public void login(User user) {
        setCurrentUser(user);
    }

    public void logout() {
        setCurrentUser(null);
    }
}