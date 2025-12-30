package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import org.project.employeemanagementsystem.model.Attendance;
import org.project.employeemanagementsystem.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.net.URL;
import java.util.ResourceBundle;

@Controller
public class AttendanceController implements Initializable {

    @Autowired private AttendanceService attendanceService;

    @FXML private TableView<Attendance> attendanceTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        attendanceTable.setItems(FXCollections.observableArrayList(attendanceService.getAllAttendanceRecords()));
    }
}