package com.payroll.model;

import java.math.BigDecimal;

public class EmployeeSalaryComponent {
    private int id;
    private int employeeId;
    private int componentId;
    private SalaryComponent component; // Embedded component definition
    private BigDecimal customValue; // Nullable; overrides default if specified
    private boolean enabled = true;

    public EmployeeSalaryComponent() {}

    public EmployeeSalaryComponent(int employeeId, int componentId, BigDecimal customValue, boolean enabled) {
        this.employeeId = employeeId;
        this.componentId = componentId;
        this.customValue = customValue;
        this.enabled = enabled;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public SalaryComponent getComponent() { return component; }
    public void setComponent(SalaryComponent component) { this.component = component; }

    public BigDecimal getCustomValue() { return customValue; }
    public void setCustomValue(BigDecimal customValue) { this.customValue = customValue; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Resolves the effective rate or fixed amount (uses custom override if present, else component default).
     */
    public BigDecimal getEffectiveValue() {
        if (customValue != null) {
            return customValue;
        }
        return component != null ? component.getDefaultValue() : BigDecimal.ZERO;
    }
}
