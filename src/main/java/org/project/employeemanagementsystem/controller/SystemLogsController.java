package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.SystemLog;
import org.project.employeemanagementsystem.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class SystemLogsController implements Initializable {

    @Autowired
    private SystemLogService systemLogService;

    // --- FXML Elements ---
    @FXML private TableView<SystemLog> logsTable;
    @FXML private TableColumn<SystemLog, String> actionColumn;
    @FXML private TableColumn<SystemLog, String> timestampColumn;
    @FXML private TableColumn<SystemLog, String> userColumn;

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
        systemLogService.clearAllLogs();
        loadLogs();
    }
}