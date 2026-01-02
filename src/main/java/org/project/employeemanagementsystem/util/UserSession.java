package org.project.employeemanagementsystem.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.project.employeemanagementsystem.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserSession {

    // 1. ΕΣΩΤΕΡΙΚΑ: Το αποθηκεύουμε ως Property για να έχουμε Listeners
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();

    // 2. ΕΞΩΤΕΡΙΚΑ (GETTER): Επιστρέφει σκέτο User, όπως ακριβώς πριν.
    // Ο υπόλοιπος κώδικας νομίζει ότι είναι απλό πεδίο.
    public User getCurrentUser() {
        return currentUser.get();
    }

    // 3. ΕΞΩΤΕΡΙΚΑ (SETTER): Δέχεται σκέτο User.
    // Ενημερώνει το Property και "πυροδοτεί" το Topbar.
    public void setCurrentUser(User user) {
        this.currentUser.set(user);
    }

    // 4. EXTRA: Μόνο το Topbar θα καλέσει αυτή τη μέθοδο για να βάλει "αφτί" (Listener)
    public ObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    // --- Legacy Methods (για να μην πειράξεις LoginController κλπ) ---

    public void login(User user) {
        setCurrentUser(user);
    }

    public void logout() {
        setCurrentUser(null);
    }
}