package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import org.project.employeemanagementsystem.model.Department;
import org.project.employeemanagementsystem.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class DepartmentController implements Initializable {
    //ΣΥΝΔΕΣΗ ΜΕ ΤΟ SERVICE. ΣΟΥ ΕΒΑΛΑ ΚΑΙ ΜΙΑ ΛΙΣΤΑ. ΑΝ ΘΕΣ ΑΛΛΑΞΕ ΤΗ
    @Autowired
    private DepartmentService departmentService;

    @FXML private TableView<Department> departmentTable;

    private ObservableList<Department> departmentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        departmentList.setAll(departmentService.getAllDepartments());
        departmentTable.setItems(departmentList);
    }
}