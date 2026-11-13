package com.payroll.model.enums;

public enum CalculationType {
    PERCENTAGE_OF_BASIC("Percentage of Basic (%)"),
    FIXED_AMOUNT("Fixed Amount");

    private final String displayName;

    CalculationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
