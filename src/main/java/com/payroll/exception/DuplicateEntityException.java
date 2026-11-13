package com.payroll.exception;

public class DuplicateEntityException extends PayrollException {
    public DuplicateEntityException(String message) {
        super(message);
    }
}
