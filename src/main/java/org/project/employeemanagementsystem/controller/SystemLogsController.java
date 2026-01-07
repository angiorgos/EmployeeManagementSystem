package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Controller
public class SystemLogsController implements Initializable {

    private final SystemLogService systemLogService;

    //FXML Elements
    @FXML private TableView<SystemLog> logsTable;
    @FXML private TableColumn<SystemLog, String> actionColumn;
    @FXML private TableColumn<SystemLog, String> timestampColumn;
    @FXML private TableColumn<SystemLog, String> userColumn;


    @Autowired
    public SystemLogsController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadLogs();
    }

    private void setupTable() {
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        userColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getUser() != null) {
                return new SimpleStringProperty(cellData.getValue().getUser().getUsername());
            } else {
                return new SimpleStringProperty("System / Unknown");
            }
        });

    }

    private void loadLogs() {
        logsTable.setItems(FXCollections.observableArrayList(systemLogService.getAllLogs()));
    }

    @FXML
    public void handleClearLogs() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear System Logs");
        alert.setHeaderText("Are you sure you want to delete ALL logs?");
        alert.setContentText("This action cannot be undone.");

        applyTheme(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            systemLogService.clearAllLogs();
            loadLogs(); // Refresh table
        }
    }


    private void applyTheme(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();


        URL cssResource = getClass().getResource("/org/project/employeemanagementsystem/view/theme.css");

        if (cssResource != null) {
            dialogPane.getStylesheets().add(cssResource.toExternalForm());
            dialogPane.getStyleClass().add("my-dialog");
        } else {
            System.err.println("Warning: theme.css not found for Alert!");
        }
    }
}