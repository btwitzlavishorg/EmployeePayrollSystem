package com.payroll.util;

import com.payroll.exception.ValidationException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9+()\\-\\s]{10,15}$"
    );

    private static final Pattern PAN_PATTERN = Pattern.compile(
            "^[A-Z]{5}[0-9]{4}[A-Z]{1}$"
    );

    private static final Pattern IFSC_PATTERN = Pattern.compile(
            "^[A-Z]{4}0[A-Z0-9]{6}$"
    );

    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required and cannot be blank.");
        }
    }

    public static void validateEmail(String email) {
        validateNotEmpty(email, "Email address");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Invalid email format: '" + email + "'. Example: name@company.com");
        }
    }

    public static void validatePhone(String phone) {
        validateNotEmpty(phone, "Phone number");
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Invalid phone number format. Must be 10-15 digits.");
        }
    }

    public static void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new ValidationException(fieldName + " cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static void validatePanNumber(String pan) {
        if (pan != null && !pan.trim().isEmpty()) {
            if (!PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches()) {
                throw new ValidationException("Invalid PAN format: '" + pan + "'. Expected format: 5 letters, 4 digits, 1 letter (e.g., ABCDE1234F).");
            }
        }
    }

    public static void validateIfsc(String ifsc) {
        if (ifsc != null && !ifsc.trim().isEmpty()) {
            if (!IFSC_PATTERN.matcher(ifsc.trim().toUpperCase()).matches()) {
                throw new ValidationException("Invalid IFSC code format: '" + ifsc + "'. Expected format: 4 letters, '0', 6 alphanumeric characters (e.g., HDFC0001234).");
            }
        }
    }
}
