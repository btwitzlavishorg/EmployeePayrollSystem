package com.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SalaryStructure {
    private int structureId;
    private int employeeId;
    private BigDecimal basicSalary;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SalaryStructure() {}

    public SalaryStructure(int employeeId, BigDecimal basicSalary, LocalDate effectiveDate) {
        this.employeeId = employeeId;
        this.basicSalary = basicSalary;
        this.effectiveDate = effectiveDate;
    }

    public int getStructureId() { return structureId; }
    public void setStructureId(int structureId) { this.structureId = structureId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
