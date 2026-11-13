package com.payroll.model.enums;

public enum LeaveType {
    CASUAL("Casual Leave (Paid)"),
    SICK("Sick Leave (Paid)"),
    PRIVILEGE("Privilege Leave (Paid)"),
    UNPAID("Loss of Pay / Unpaid Leave");

    private final String displayName;

    LeaveType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isLossOfPay() {
        return this == UNPAID;
    }
}
