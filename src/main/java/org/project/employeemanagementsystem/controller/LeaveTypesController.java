package org.project.employeemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class LeaveTypesController implements Initializable {

    @FXML
    private TableView<?> leaveTypesTable;

    @FXML
    private TableColumn<?, ?> leaveIDColumn;

    @FXML
    private TableColumn<?, ?> leaveNameColumn;

    @FXML
    private TableColumn<?, ?> leaveMaxDaysColumn;

    @FXML
    private TextField leaveNameField;

    @FXML
    private TextField leaveMaxDaysField;

    @FXML
    private Button leaveTypeRemoveBtn;

    @FXML
    private Button leaveTypeSaveBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // intentionally empty (matches your request)
    }
}
