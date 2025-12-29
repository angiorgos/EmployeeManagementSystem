package org.project.employeemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.project.employeemanagementsystem.model.Holiday;
import org.project.employeemanagementsystem.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Controller
public class HolidaysController implements Initializable {

    @Autowired
    private HolidayService holidayService;

    @FXML private TableView<Holiday> holidaysTable;
    @FXML private TableColumn<Holiday, String> nameColumn;
    @FXML private TableColumn<Holiday, LocalDate> dateColumn;

    @FXML private TextField nameField;
    @FXML private DatePicker datePicker;
    @FXML private Label statusLabel;

    private ObservableList<Holiday> holidayList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        loadHolidays();
    }

    private void loadHolidays() {
        holidayList.setAll(holidayService.getAllHolidays());
        holidaysTable.setItems(holidayList);
    }

}