package com.payroll.model;

import com.payroll.model.enums.CalculationType;
import com.payroll.model.enums.ComponentType;
import java.math.BigDecimal;

public class SalaryComponent {
    private int componentId;
    private String componentCode;
    private String componentName;
    private ComponentType componentType;
    private CalculationType calculationType;
    private BigDecimal defaultValue = BigDecimal.ZERO;
    private boolean active = true;
    private String description;

    public SalaryComponent() {}

    public SalaryComponent(int componentId, String componentCode, String componentName,
                           ComponentType componentType, CalculationType calculationType,
                           BigDecimal defaultValue, boolean active, String description) {
        this.componentId = componentId;
        this.componentCode = componentCode;
        this.componentName = componentName;
        this.componentType = componentType;
        this.calculationType = calculationType;
        this.defaultValue = defaultValue;
        this.active = active;
        this.description = description;
    }

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public ComponentType getComponentType() { return componentType; }
    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }

    public CalculationType getCalculationType() { return calculationType; }
    public void setCalculationType(CalculationType calculationType) { this.calculationType = calculationType; }

    public BigDecimal getDefaultValue() { return defaultValue; }
    public void setDefaultValue(BigDecimal defaultValue) { this.defaultValue = defaultValue; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEarning() {
        return componentType == ComponentType.EARNING;
    }

    public boolean isDeduction() {
        return componentType == ComponentType.DEDUCTION;
    }

    @Override
    public String toString() {
        return componentName + " (" + componentType.getDisplayName() + ")";
    }
}
