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

    private List<LeaveRequest> allRequests;

    @FXML
    public void initialize() {
        allRequests = leaveRequestService.getAllRequests();

        // Create a list including "All" (null) at the start
        List<LeaveStatus> statuses = new ArrayList<>();
        statuses.add(null); // represents "All"
        statuses.addAll(List.of(LeaveStatus.values()));

        statusFilterCombo.setItems(FXCollections.observableArrayList(statuses));

        // Show "All" by default
        statusFilterCombo.setValue(null);
        statusFilterCombo.setPromptText("All");

        // Listeners for search and filter
        searchField.textProperty().addListener((obs, o, n) -> refresh());
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> refresh());

        requestsFlowPane.setHgap(15);
        requestsFlowPane.setVgap(15);
        requestsFlowPane.setPadding(new Insets(10));
        refresh();
    }

    private void refresh() {
        requestsFlowPane.getChildren().clear();

        String search = searchField.getText().toLowerCase();
        LeaveStatus selectedStatus = statusFilterCombo.getValue();

        List<LeaveRequest> filtered = allRequests.stream()
                .filter(r -> (selectedStatus == null || r.getStatus() == selectedStatus)
                        && (r.getEmployee().getFirstName() + " " + r.getEmployee().getLastName())
                        .toLowerCase()
                        .contains(search))
                .collect(Collectors.toList());

        filtered.forEach(this::createRequestCard);
    }

    private void createRequestCard(LeaveRequest request) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setSpacing(5);

        // Fixed width so background shows
        card.setMinWidth(200);
        card.setPrefWidth(200);
        card.setMaxWidth(200);

        card.setStyle(getStatusStyle(request.getStatus()));

        Label title = new Label("REQUEST " + request.getId());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label name = new Label("Name: " + request.getEmployee().getFirstName() + " " + request.getEmployee().getLastName());
        Label phone = new Label("Phone: " + request.getEmployee().getPhone());
        Label email = new Label("Email: " + request.getEmployee().getEmail());

        Button reasonBtn = new Button("Show Reason");
        reasonBtn.setStyle("-fx-background-color: white; -fx-border-color: #D1D5DB; -fx-font-weight: bold; -fx-padding: 6 16;");
        reasonBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Leave Reason");
            alert.setHeaderText(null);
            alert.setContentText(request.getReason());
            alert.showAndWait();
        });

        ComboBox<LeaveStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(LeaveStatus.values()));
        statusCombo.setValue(request.getStatus());
        statusCombo.setOnAction(e -> {
            LeaveStatus newStatus = statusCombo.getValue();
            request.setStatus(newStatus);
            leaveRequestService.submitRequest(request);
            card.setStyle(getStatusStyle(newStatus));
        });

        card.getChildren().addAll(title, name, phone, email, reasonBtn, statusCombo);
        FlowPane.setMargin(card, new Insets(5));
        requestsFlowPane.getChildren().add(card);
    }

    private String getStatusStyle(LeaveStatus status) {
        String base = "-fx-border-radius: 5; -fx-border-color: gray; -fx-background-radius: 5; -fx-padding: 10;";
        if (status == null) return base + " -fx-background-color: #f9f9f9;";
        switch (status) {
            case PENDING: return base + " -fx-background-color: #FFFBEB;";
            case APPROVED: return base + " -fx-background-color: #DCFCE7;";
            case REJECTED: return base + " -fx-background-color: #FEF2F2;";
            default: return base + " -fx-background-color: #f9f9f9;";
        }
    }
}
