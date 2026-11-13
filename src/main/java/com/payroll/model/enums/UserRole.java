package com.payroll.model.enums;

public enum UserRole {
    ADMIN("Administrator"),
    HR_MANAGER("HR Manager"),
    ACCOUNTANT("Accountant");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
