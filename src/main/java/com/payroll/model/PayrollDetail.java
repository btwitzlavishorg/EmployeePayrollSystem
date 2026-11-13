package com.payroll.model;

import com.payroll.model.enums.ComponentType;
import java.math.BigDecimal;

public class PayrollDetail {
    private int detailId;
    private int payrollId;
    private String componentName;
    private ComponentType componentType;
    private BigDecimal amount = BigDecimal.ZERO;

    public PayrollDetail() {}

    public PayrollDetail(String componentName, ComponentType componentType, BigDecimal amount) {
        this.componentName = componentName;
        this.componentType = componentType;
        this.amount = amount;
    }

    public PayrollDetail(int detailId, int payrollId, String componentName, ComponentType componentType, BigDecimal amount) {
        this.detailId = detailId;
        this.payrollId = payrollId;
        this.componentName = componentName;
        this.componentType = componentType;
        this.amount = amount;
    }

    public int getDetailId() { return detailId; }
    public void setDetailId(int detailId) { this.detailId = detailId; }

    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int payrollId) { this.payrollId = payrollId; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public ComponentType getComponentType() { return componentType; }
    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public boolean isEarning() {
        return componentType == ComponentType.EARNING;
    }

    public boolean isDeduction() {
        return componentType == ComponentType.DEDUCTION;
    }
}
