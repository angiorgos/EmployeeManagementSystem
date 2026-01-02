package org.project.employeemanagementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.project.employeemanagementsystem.util.AuditListener;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private LocalDate date; // Η ημερομηνία της αργίας
}