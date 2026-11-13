package com.payroll.model.enums;

public enum ComponentType {
    EARNING("Earning / Allowance"),
    DEDUCTION("Deduction");

    private final String displayName;

    ComponentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
