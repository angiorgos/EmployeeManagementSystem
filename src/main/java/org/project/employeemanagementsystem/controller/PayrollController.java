package org.project.employeemanagementsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.project.employeemanagementsystem.model.Employee;
import org.project.employeemanagementsystem.model.Payment;
import org.project.employeemanagementsystem.service.EmployeeService;
import org.project.employeemanagementsystem.service.PaymentService;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class PayrollController implements Initializable {

    private final PaymentService paymentService;
    private final EmployeeService employeeService;

    public PayrollController(PaymentService paymentService, EmployeeService employeeService) {
        this.paymentService = paymentService;
        this.employeeService = employeeService;
    }

    // --- FXML Elements ---
    @FXML private VBox settingsPanel;
    @FXML private TextField txtOvertimeRate;
    @FXML private TextField txtSundayRate;
    @FXML private TextField txtInsuranceRate;

    // Table
    @FXML private TableView<Payment> payrollTable;
    @FXML private TableColumn<Payment, Long> colId;
    @FXML private TableColumn<Payment, String> colName;
    @FXML private TableColumn<Payment, Double> colAmount; // Net Pay
    @FXML private TableColumn<Payment, LocalDate> colDate;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, Void> colActions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Default Values
        txtOvertimeRate.setText("1.50"); // 50% προσαύξηση
        txtSundayRate.setText("1.75");   // 75% προσαύξηση
        txtInsuranceRate.setText("0.16"); // 16% ΙΚΑ εργαζόμενου (τυχαίο παράδειγμα)

        setupTableColumns();
        refreshTable();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Σύνδεση με το όνομα του Υπαλλήλου
        colName.setCellValueFactory(cellData -> {
            Employee emp = cellData.getValue().getEmployee();
            return new SimpleStringProperty(emp != null ? emp.getLastName() + " " + emp.getFirstName() : "Unknown");
        });

        // Προσοχή: Εδώ τραβάμε το "amount" που είναι το Καθαρό Πληρωτέο
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Ρύθμιση του κουμπιού "Details"
        colActions.setCellFactory(getButtonCellFactory());
    }

    private void refreshTable() {
        List<Payment> payments = paymentService.getAllPayments();
        payrollTable.setItems(FXCollections.observableArrayList(payments));
    }

    @FXML
    public void generatePayroll() {
        try {
            double otRate = Double.parseDouble(txtOvertimeRate.getText());
            double sundayRate = Double.parseDouble(txtSundayRate.getText());
            double insurance = Double.parseDouble(txtInsuranceRate.getText());

            List<Employee> employees = employeeService.getAllEmployees();

            // ΣΕΝΑΡΙΟ: Υπολογισμός μισθοδοσίας για όλους
            // Σημείωση: Εδώ κανονικά θα τραβούσες τις πραγματικές ώρες από το AttendanceService
            // Για το παράδειγμα, βάζουμε τυχαίες υπερωρίες για να δεις νούμερα
            for (Employee emp : employees) {
                // Παράδειγμα: Όλοι δούλεψαν 176 ώρες, + 5 ώρες υπερωρία, + 0 Κυριακές
                paymentService.calculateAndSavePayroll(emp, 176.0, 5.0, 0.0, otRate, sundayRate, insurance);
            }

            refreshTable();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Payroll Generated Successfully!");
            alert.show();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Please enter valid numbers in settings.").show();
        }
    }

    @FXML
    public void toggleSettings() {
        boolean isVisible = settingsPanel.isVisible();
        settingsPanel.setVisible(!isVisible);
        settingsPanel.setManaged(!isVisible);
    }

    // --- Button Factory για το "Details" ---
    private Callback<TableColumn<Payment, Void>, TableCell<Payment, Void>> getButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Payment, Void> call(final TableColumn<Payment, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Details");
                    {
                        btn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                        btn.setOnAction(event -> showPaymentDetails(getTableView().getItems().get(getIndex())));
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btn);
                    }
                };
            }
        };
    }

    // --- Το Popup με την ανάλυση ---
    private void showPaymentDetails(Payment p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Details");
        alert.setHeaderText("Payroll for: " + p.getEmployee().getLastName() + " " + p.getEmployee().getFirstName());

        // Εμφανίζουμε τα νέα πεδία από το Entity
        String content = String.format(
                "Base Salary:      €%.2f\n" +
                        "Month:            %s\n" +
                        "-----------------------------\n" +
                        "Hours Worked:     %.1f hrs\n" +
                        "Overtime Hours:   %.1f hrs\n" +
                        "Sunday Hours:     %.1f hrs\n" +
                        "-----------------------------\n" +
                        "GROSS PAY:        €%.2f\n" +
                        "Deductions:      -€%.2f\n" +
                        "-----------------------------\n" +
                        "NET PAY:          €%.2f",
                p.getBaseSalary(),
                p.getMonthYear(),
                p.getHoursWorked(),
                p.getOvertimeHours(),
                p.getSundayHours(),
                p.getGrossPay(),
                p.getDeductions(),
                p.getAmount()
        );

        alert.setContentText(content);
        alert.showAndWait();
    }


    @FXML
    public void saveSettings() {
        // Επειδή η μέθοδος generatePayroll() διαβάζει απευθείας από τα TextFields,
        // το Save απλά κλείνει το παράθυρο των ρυθμίσεων.
        toggleSettings();

        // Προαιρετικά: Μπορείς να εμφανίσεις ένα μήνυμα επιβεβαίωσης
        // System.out.println("Settings saved temporarily.");
    }
}