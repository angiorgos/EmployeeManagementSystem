package org.project.employeemanagementsystem.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class LogsController implements Initializable {

    @Autowired
    private SystemLogService logService;

    @FXML private VBox loadingOverlay;
    @FXML private TextField searchField;
    @FXML private DatePicker dateFilter;

    @FXML private TableView<SystemLog> logsTable;
    @FXML private TableColumn<SystemLog, String> colTime;
    @FXML private TableColumn<SystemLog, String> colUser;
    @FXML private TableColumn<SystemLog, String> colAction;

    private final ObservableList<SystemLog> masterData = FXCollections.observableArrayList();
    private FilteredList<SystemLog> filteredData;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupFilters();
        loadLogs();
    }

    private void setupTable() {
        // Στήλη Ώρας
        colTime.setCellValueFactory(c -> {
            if (c.getValue().getTimestamp() != null)
                return new SimpleStringProperty(c.getValue().getTimestamp().format(FMT));
            return new SimpleStringProperty("-");
        });

        // Στήλη Χρήστη
        colUser.setCellValueFactory(c -> {
            String displayUser = c.getValue().getUsername();
            if (displayUser == null && c.getValue().getUser() != null) {
                displayUser = c.getValue().getUser().getUsername();
            }
            return new SimpleStringProperty(displayUser != null ? displayUser : "System");
        });

        // Στήλη Ενέργειας
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));

        //για σφάλματα
        colAction.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.toLowerCase().contains("failed") || item.toLowerCase().contains("error")) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        logsTable.setPlaceholder(new Label("No logs found matching your criteria."));
    }

    private void setupFilters() {
        filteredData = new FilteredList<>(masterData, p -> true);
        logsTable.setItems(filteredData);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    private void updatePredicate() {
        String searchText = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
        LocalDate filterDate = dateFilter.getValue();

        filteredData.setPredicate(log -> {
            if (filterDate != null) {
                if (log.getTimestamp() == null) return false;
                if (!log.getTimestamp().toLocalDate().equals(filterDate)) {
                    return false;
                }
            }
            if (!searchText.isEmpty()) {
                String action = (log.getAction() != null) ? log.getAction().toLowerCase() : "";
                String user = (log.getUsername() != null) ? log.getUsername().toLowerCase() : "";

                return action.contains(searchText) || user.contains(searchText);
            }

            return true;
        });
    }

    @FXML
    private void onRefresh() {
        loadLogs();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        dateFilter.setValue(null);
    }

    @FXML
    private void onClearLogs() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Audit Trail");
        alert.setContentText("Are you sure you want to permanently delete all log history? This action cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            logService.clearAllLogs();
            loadLogs();
        }
    }

    private void loadLogs() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            loadingOverlay.setOpacity(1.0);
        }

        PauseTransition delay = new PauseTransition(Duration.millis(50));
        delay.setOnFinished(ev -> {
            Task<List<SystemLog>> task = new Task<>() {
                @Override
                protected List<SystemLog> call() {
                    return logService.getAllLogs();
                }
            };

            task.setOnSucceeded(e -> {
                masterData.setAll(task.getValue());

                if (loadingOverlay != null) {
                    FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), loadingOverlay);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(evt -> loadingOverlay.setVisible(false));
                    fadeOut.play();
                }
            });

            task.setOnFailed(e -> {
                if (loadingOverlay != null) loadingOverlay.setVisible(false);
                e.getSource().getException().printStackTrace();
            });

            new Thread(task).start();
        });
        delay.play();
    }
}