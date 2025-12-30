package org.project.employeemanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Σύνδεση με Υπάλληλο
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // --- Ημερομηνίες ---
    private LocalDate paymentDate; // Πότε έγινε η πληρωμή (π.χ. 2024-05-30)
    private String monthYear;      // Για ποιον μήνα αφορά (π.χ. "05/2024")

    // --- Τα Οικονομικά Στοιχεία (ΓΙΑ ΝΑ ΕΧΟΥΜΕ ΙΣΤΟΡΙΚΟ) ---

    private Double baseSalary;     // Ο βασικός μισθός που είχε τότε

    // Ώρες (Για να ξέρουμε πώς δούλεψε)
    private Double hoursWorked;    // Κανονικές ώρες
    private Double overtimeHours;  // Υπερωρίες
    private Double sundayHours;    // Κυριακές/Αργίες

    // Υπολογισμοί
    private Double grossPay;       // Μικτά (Πριν τις κρατήσεις)
    private Double deductions;     // Κρατήσεις (Ασφάλεια/Φόροι)

    private Double amount;         // Το ΤΕΛΙΚΟ ΠΟΣΟ (Καθαρά - Net Pay)

    // Κατάσταση
    private String status;         // "PENDING", "PAID", "CANCELLED"
}