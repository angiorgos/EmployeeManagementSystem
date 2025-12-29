package org.project.employeemanagementsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employee employee;

    public boolean isActive(){
        // Για admin λογαριασμό που δεν αντιστοιχεί απαραίτητα σε υπάλληλο
        if(this.employee == null){
            return true;
        }
        //Είσοδος μόνο αν δεν έχει ημερομηνία εξόδου.
        return this.employee.getExitDate() == null;
    }
}