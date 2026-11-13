package com.payroll.model.enums;

public enum EmploymentStatus {
    ACTIVE("Active"),
    ON_LEAVE("On Leave"),
    TERMINATED("Terminated");

    private final String displayName;

    EmploymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
