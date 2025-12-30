package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class EmployeeController implements Initializable {

    @Autowired
    private EmployeeService employeeService;
    //Περικλή σου δίνω κάποια βασικα πεδία που ίσως χρειαστείς. Μπορεις να τα αλλαξεις
    @FXML private TableView<Employee> employeesTable;
    @FXML private TableColumn<Employee, String> firstNameCol;
    @FXML private TableColumn<Employee, String> lastNameCol;
    @FXML private TableColumn<Employee, String> emailCol;
    @FXML private TableColumn<Employee, String> departmentCol;

    //Ορίζω τη λίστα με ολους τους υπαλλήλους.
    private ObservableList<Employee> employeeList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    private void loadData() {
        // Τραβάει τα δεδομένα από τη βάση μέσω του Service
        employeeList.setAll(employeeService.getAllEmployees());
        employeesTable.setItems(employeeList);
    }
}