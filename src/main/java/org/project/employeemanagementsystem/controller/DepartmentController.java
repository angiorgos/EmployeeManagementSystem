package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class DepartmentController implements Initializable {
    //ΣΥΝΔΕΣΗ ΜΕ ΤΟ SERVICE. ΣΟΥ ΕΒΑΛΑ ΚΑΙ ΜΙΑ ΛΙΣΤΑ. ΑΝ ΘΕΣ ΑΛΛΑΞΕ ΤΗ
    @Autowired
    private DepartmentService departmentService;

    //TODO LIST FOR 31/12 NA PO TON GEORGE AN POS NA TAVAO TON ARITHMO TON EMPLOYESS APO THN VASI

  //  @FXML private TableView<Department> departmentTable;

    // TableView and columns
    @FXML private TableView<Department> departmentTable;
    @FXML private TableColumn<Department, Long> idCol;
    @FXML private TableColumn<Department, String> nameCol;


    // Input fields
    @FXML private TextField DepartmentNameTextArea;
    @FXML private TextArea DepartmentDescriptionTextArea;

    // Buttons
    @FXML private Button CreateDepartmentButton;
    @FXML private Button EditDepartmentButton;
    @FXML private Button DeleteDepartMentButton;

    private ObservableList<Department> departmentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Map entity fields to table columns
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        departmentList.setAll(departmentService.getAllDepartments());
        departmentTable.setItems(departmentList);
    }
}