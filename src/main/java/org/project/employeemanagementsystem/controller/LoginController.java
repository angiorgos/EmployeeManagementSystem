package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.project.employeemanagementsystem.service.UserService;
import org.springframework.stereotype.Component;

@Component // Απαραίτητο για να το βλέπει το Spring
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService;

    // Constructor Injection (Το Spring σου δίνει το UserService)
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (userService.authenticate(username, password)) {
            errorLabel.setText("Επιτυχία! Συνδέθηκες.");
            errorLabel.setStyle("-fx-text-fill: green;");
            // Εδώ αργότερα θα βάλουμε κώδικα που ανοίγει το Main Dashboard
        } else {
            errorLabel.setText("Λάθος όνομα χρήστη ή κωδικός.");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }
}