package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginController extends BaseController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @Autowired
    private AuthService authService;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Fill all the fields");
            return;
        }

        User user = authService.login(username, password);

        if (user != null) {
            userSession.login(user);
            navigator.goToDashboard();
            navigator.maximize();
        } else {
            errorLabel.setText("Wrong credentials");
        }
    }
}