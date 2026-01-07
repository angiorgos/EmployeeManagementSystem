package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.service.AuthService;
import org.project.employeemanagementsystem.service.SystemLogService;
import org.project.employeemanagementsystem.util.Navigator;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController {

    @Autowired private AuthService authService;
    @Autowired private Navigator navigator;
    @Autowired private UserSession userSession;
    @Autowired private org.project.employeemanagementsystem.service.SystemLogService logService;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        User user = authService.authenticate(username, password);

        if (user != null) {
            userSession.login(user);
            logService.log("User Login Success");
            navigator.goToDashboard();
        } else {
            logService.log("Failed login attempt for username: " + username);
            errorLabel.setText("Wrong credentials!");
        }
    }
}