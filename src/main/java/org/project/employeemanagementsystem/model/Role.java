package org.project.employeemanagementsystem.model;

public enum Role {
    ADMIN("Διαχειριστής Συστήματος"),
    HR("HR Manager"),
    ACCOUNTANT("Λογιστής");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}