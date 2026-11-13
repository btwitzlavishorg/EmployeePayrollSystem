package com.payroll.exception;

/**
 * Base unchecked exception for all business and operational exceptions
 * within the Employee Payroll Management System.
 */
public class PayrollException extends RuntimeException {
    
    public PayrollException(String message) {
        super(message);
    }

    public PayrollException(String message, Throwable cause) {
        super(message, cause);
    }
}
