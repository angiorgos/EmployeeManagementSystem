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
        // Load all requests from DB
        allRequests = leaveRequestService.getAllRequests();

        // Populate ComboBox with "All" (null) + actual statuses
        List<LeaveStatus> statuses = new ArrayList<>();
        statuses.add(null); // Represents "All"
        statuses.addAll(List.of(LeaveStatus.values()));
        statusFilterCombo.setItems(FXCollections.observableArrayList(statuses));

        statusFilterCombo.setValue(null); // Default = "All"
        statusFilterCombo.setPromptText("All");

        // Listeners for live filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refresh());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> refresh());

        // Refresh button
        refreshBtn.setOnAction(e -> refresh());

        // FlowPane spacing
        requestsFlowPane.setHgap(15);
        requestsFlowPane.setVgap(15);
        requestsFlowPane.setPadding(new Insets(10));

        refresh();
    }

    /** Refresh all cards based on search text and status filter */
    private void refresh() {
        requestsFlowPane.getChildren().clear();

        String search = searchField.getText().toLowerCase().trim();
        LeaveStatus selectedStatus = statusFilterCombo.getValue();

        List<LeaveRequest> filtered = allRequests.stream()
                .filter(r -> (selectedStatus == null || r.getStatus() == selectedStatus) &&
                        (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName())
                                .toLowerCase()
                                .contains(search))
                .collect(Collectors.toList());

        filtered.forEach(this::createRequestCard);
    }

    /** Create a card VBox for a single leave request */
    private void createRequestCard(LeaveRequest request) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setSpacing(5);

        // Fixed width so background shows clearly
        card.setMinWidth(200);
        card.setPrefWidth(200);
        card.setMaxWidth(200);

        // Set initial background color based on status
        card.setStyle(getStatusStyle(request.getStatus()));

        // Labels
        Label title = new Label("REQUEST " + request.getId());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label name = new Label("Name: " + request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName());
        Label phone = new Label("Phone: " + request.getEmployee().getPhone());
        Label email = new Label("Email: " + request.getEmployee().getEmail());

        // Show Reason Button
        Button reasonBtn = new Button("Show Reason");
        reasonBtn.getStyleClass().add("btn-secondary");
        reasonBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Leave Reason");
            alert.setHeaderText(null);
            alert.setContentText(request.getReason());
            alert.showAndWait();
        });

        // Status ComboBox for approving/rejecting
        ComboBox<LeaveStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(LeaveStatus.values()));
        statusCombo.setValue(request.getStatus());
        statusCombo.setOnAction(e -> {
            LeaveStatus newStatus = statusCombo.getValue();
            request.setStatus(newStatus);

            // Persist status change to database
            leaveRequestService.submitRequest(request);

            // Update card color immediately
            card.setStyle(getStatusStyle(newStatus));
        });

        // Add all components to the card
        card.getChildren().addAll(title, name, phone, email, reasonBtn, statusCombo);
        FlowPane.setMargin(card, new Insets(5));

        requestsFlowPane.getChildren().add(card);
    }

    /** Inline styles for each status */
    private String getStatusStyle(LeaveStatus status) {
        String base = "-fx-border-radius: 5; -fx-background-radius: 5; -fx-border-color: gray; -fx-padding: 10;";
        if (status == null) return base + " -fx-background-color: #f9f9f9;";       // "All" or unassigned
        switch (status) {
            case PENDING: return base + " -fx-background-color: #FFF3CD;";  // light yellow
            case APPROVED: return base + " -fx-background-color: #DCFCE7;"; // light green
            case REJECTED: return base + " -fx-background-color: #FECACA;"; // light red
            default: return base + " -fx-background-color: #f9f9f9;";
        }
    }

    /** Handler for Refresh button (optional explicit method for FXML) */
    @FXML
    private void onRefresh() {
        // Re-fetch requests from DB in case any changes happened elsewhere
        allRequests = leaveRequestService.getAllRequests();
        refresh();
    }
}
