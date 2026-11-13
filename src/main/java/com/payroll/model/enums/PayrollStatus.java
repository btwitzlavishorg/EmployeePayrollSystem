package com.payroll.model.enums;

public enum PayrollStatus {
    PENDING("Pending"),
    PROCESSED("Processed"),
    PAID("Paid");

    private final String displayName;

    PayrollStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
