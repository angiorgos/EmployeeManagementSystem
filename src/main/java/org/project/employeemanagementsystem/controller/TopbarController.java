package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.project.employeemanagementsystem.model.User;
import org.project.employeemanagementsystem.util.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class TopbarController implements Initializable {

    private final UserSession userSession;

    // FXML Elements
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Circle userAvatar;       // Η φωτογραφία
    @FXML private FontIcon defaultUserIcon; // Το default εικονίδιο

    @Autowired
    public TopbarController(UserSession userSession) {
        this.userSession = userSession;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateUserDisplay();
    }

    private void updateUserDisplay() {
        User currentUser = userSession.getCurrentUser();

        if (currentUser != null) {
            // 1. Ρύθμιση Ονόματος
            if (currentUser.getEmployee() != null) {
                String fullName = currentUser.getEmployee().getFirstName() + " " +
                        currentUser.getEmployee().getLastName();
                userNameLabel.setText(fullName);
            } else {
                userNameLabel.setText(currentUser.getUsername());
            }

            // 2. Ρύθμιση Ρόλου
            if (currentUser.getRole() != null) {
                userRoleLabel.setText(currentUser.getRole().getName());
            } else {
                userRoleLabel.setText("-");
            }

            // 3. Ρύθμιση Φωτογραφίας
            if (currentUser.getProfilePicture() != null && currentUser.getProfilePicture().length > 0) {
                try {
                    // Μετατροπή bytes σε εικόνα
                    Image img = new Image(new ByteArrayInputStream(currentUser.getProfilePicture()));

                    // Γέμισμα του κύκλου με την εικόνα
                    userAvatar.setFill(new ImagePattern(img));

                    // Εμφάνιση Avatar, Απόκρυψη Icon
                    userAvatar.setVisible(true);
                    defaultUserIcon.setVisible(false);
                } catch (Exception e) {
                    System.err.println("Error loading topbar image: " + e.getMessage());
                    showDefaultIcon();
                }
            } else {
                // Δεν υπάρχει φωτογραφία -> Δείξε το default icon
                showDefaultIcon();
            }
        }
    }

    private void showDefaultIcon() {
        userAvatar.setVisible(false);
        defaultUserIcon.setVisible(true);
    }
}