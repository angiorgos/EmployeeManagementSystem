package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import org.project.employeemanagementsystem.service.UserService;
import org.springframework.stereotype.Component;

@Component // Σημαντικό: Για να το βλέπει το Spring
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService;

    // Ζητάμε το Service, όχι το DAO!
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @FXML
    public void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (userService.authenticate(user, pass)) {
            errorLabel.setText("Επιτυχία!");
            // Κώδικας για αλλαγή σκηνής σε Dashboard...
        } else {
            errorLabel.setText("Λάθος στοιχεία.");
        }
    }
}