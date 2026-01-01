package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.project.employeemanagementsystem.model.LeaveRequest;
import org.project.employeemanagementsystem.model.LeaveStatus;
import org.project.employeemanagementsystem.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LeavesController2 {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @FXML private FlowPane requestsFlowPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<LeaveStatus> statusFilterCombo;
    @FXML private Button refreshBtn;

    private List<LeaveRequest> allRequests;

    @FXML
    public void initialize() {
        // 1. Setup Status Filter
        List<LeaveStatus> statuses = new ArrayList<>();
        statuses.add(null); // "All"
        statuses.addAll(List.of(LeaveStatus.values()));
        statusFilterCombo.setItems(FXCollections.observableArrayList(statuses));
        statusFilterCombo.setPromptText("All");

        // 2. Setup Listeners for Filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshUIOnly());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshUIOnly());

        // 3. Setup FlowPane
        requestsFlowPane.setHgap(15);
        requestsFlowPane.setVgap(15);
        requestsFlowPane.setPadding(new Insets(10));

        // 4. Initial Load
        loadDataFromDB();
    }

    @FXML
    private void onRefresh() {
        loadDataFromDB();
    }

    /** 1. Φορτώνει τα δεδομένα από τη βάση */
    private void loadDataFromDB() {
        allRequests = leaveRequestService.getAllRequests();
        refreshUIOnly(); // Μετά το load, ζωγράφισε τα
    }

    /** 2. Ζωγραφίζει τις κάρτες (Φιλτράρισμα στη μνήμη) */
    private void refreshUIOnly() {
        requestsFlowPane.getChildren().clear();

        if (allRequests == null) return;

        String search = (searchField.getText() != null) ? searchField.getText().toLowerCase().trim() : "";
        LeaveStatus selectedStatus = statusFilterCombo.getValue();

        List<LeaveRequest> filtered = allRequests.stream()
                .filter(r -> (selectedStatus == null || r.getStatus() == selectedStatus))
                .filter(r -> {
                    String fullName = (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName()).toLowerCase();
                    return fullName.contains(search);
                })
                .collect(Collectors.toList());

        filtered.forEach(this::createRequestCard);
    }

    /** Δημιουργία Κάρτας */
    private void createRequestCard(LeaveRequest request) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setSpacing(8); // Λίγο περισσότερο κενό

        // Σταθερό μέγεθος για ομοιομορφία
        card.setMinWidth(220);
        card.setPrefWidth(220);
        card.setMaxWidth(220);

        // Στυλ
        card.setStyle(getStatusStyle(request.getStatus()));

        // Labels
        Label title = new Label("REQ #" + request.getId() + " - " + request.getLeaveType().getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label name = new Label(request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label dates = new Label(request.getStartDate() + " to " + request.getEndDate());
        dates.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");

        // Reason Button
        Button reasonBtn = new Button("View Reason");
        reasonBtn.setMaxWidth(Double.MAX_VALUE); // Stretch
        reasonBtn.getStyleClass().add("btn-secondary"); // Αν έχεις theme, αλλιώς βγάλτο
        reasonBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Leave Reason");
            alert.setHeaderText("Reason for Request #" + request.getId());
            alert.setContentText(request.getReason());
            alert.showAndWait();
        });

        // Status ComboBox for Actions
        ComboBox<LeaveStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(LeaveStatus.values()));
        statusCombo.setValue(request.getStatus());
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        // --- Η ΚΡΙΣΙΜΗ ΑΛΛΑΓΗ ΕΔΩ ---
        statusCombo.setOnAction(e -> {
            LeaveStatus newStatus = statusCombo.getValue();

            // 1. Ενημερώνουμε το αντικείμενο
            request.setStatus(newStatus);

            try {
                // 2. Καλούμε τη ΣΩΣΤΗ μέθοδο update (όχι submit)
                leaveRequestService.updateRequestStatus(request);

                // 3. Αλλάζουμε χρώμα επιτόπου
                card.setStyle(getStatusStyle(newStatus));
                System.out.println("Update success for REQ #" + request.getId());

            } catch (Exception ex) {
                ex.printStackTrace();
                // Αν αποτύχει, γύρνα το ComboBox πίσω στο παλιό
                statusCombo.setValue(request.getStatus());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Update failed: " + ex.getMessage());
                alert.show();
            }
        });

        // Add to Card
        card.getChildren().addAll(title, name, dates, reasonBtn, new Separator(), new Label("Set Status:"), statusCombo);

        // Add to FlowPane
        requestsFlowPane.getChildren().add(card);
    }

    private String getStatusStyle(LeaveStatus status) {
        String base = "-fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); -fx-padding: 10;";
        if (status == null) return base + " -fx-background-color: #f9f9f9;";
        switch (status) {
            case PENDING:  return base + " -fx-background-color: #FFF3CD; -fx-border-color: #FFC107;"; // Yellow
            case APPROVED: return base + " -fx-background-color: #DCFCE7; -fx-border-color: #22C55E;"; // Green
            case REJECTED: return base + " -fx-background-color: #FEE2E2; -fx-border-color: #EF4444;"; // Red
            default:       return base + " -fx-background-color: #f9f9f9;";
        }
    }
}