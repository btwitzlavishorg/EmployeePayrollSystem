-- ====================================================================
-- Employee Payroll Management System - Relational Database Schema
-- Database: MySQL 8.0+ / MariaDB / ANSI SQL Compatible
-- ====================================================================

CREATE DATABASE IF NOT EXISTS payroll_db;
USE payroll_db;

-- 1. USERS TABLE (System Administrators & HR Users)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN', -- ADMIN, HR_MANAGER, ACCOUNTANT
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, LOCKED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. DEPARTMENTS TABLE
CREATE TABLE IF NOT EXISTS departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_code VARCHAR(10) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. EMPLOYEES TABLE
CREATE TABLE IF NOT EXISTS employees (
    employee_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL UNIQUE, -- e.g. EMP1001
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    address TEXT,
    department_id INT NOT NULL,
    designation VARCHAR(50) NOT NULL,
    date_of_joining DATE NOT NULL,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ON_LEAVE, TERMINATED
    bank_name VARCHAR(100) NOT NULL,
    bank_account_no VARCHAR(30) NOT NULL,
    bank_ifsc VARCHAR(20) NOT NULL,
    pan_no VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_department FOREIGN KEY (department_id) 
        REFERENCES departments (department_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 4. SALARY STRUCTURES TABLE (Base salary configuration per employee)
CREATE TABLE IF NOT EXISTS salary_structures (
    structure_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL UNIQUE,
    basic_salary DECIMAL(12, 2) NOT NULL CHECK (basic_salary >= 0),
    effective_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sal_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (employee_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 5. SALARY COMPONENTS TABLE (Catalog of global allowances and deductions)
CREATE TABLE IF NOT EXISTS salary_components (
    component_id INT AUTO_INCREMENT PRIMARY KEY,
    component_code VARCHAR(20) NOT NULL UNIQUE,
    component_name VARCHAR(100) NOT NULL,
    component_type VARCHAR(20) NOT NULL, -- EARNING, DEDUCTION
    calculation_type VARCHAR(30) NOT NULL, -- PERCENTAGE_OF_BASIC, FIXED_AMOUNT
    default_value DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255)
);

-- 6. EMPLOYEE SALARY COMPONENTS (Configured allowances/deductions per employee)
CREATE TABLE IF NOT EXISTS employee_salary_components (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    component_id INT NOT NULL,
    custom_value DECIMAL(10, 2) NULL, -- Overrides component default if not null
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_esc_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (employee_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_esc_component FOREIGN KEY (component_id) 
        REFERENCES salary_components (component_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_emp_component UNIQUE (employee_id, component_id)
);

-- 7. ATTENDANCE TABLE (Daily records for tracking present/absent days)
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT', -- PRESENT, ABSENT, HALF_DAY, LEAVE
    check_in_time TIME NULL,
    check_out_time TIME NULL,
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (employee_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_emp_date UNIQUE (employee_id, attendance_date)
);

-- 8. LEAVE REQUESTS TABLE
CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    leave_type VARCHAR(20) NOT NULL, -- CASUAL, SICK, UNPAID, PRIVILEGE
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INT NOT NULL CHECK (total_days > 0),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_by INT NULL,
    reviewed_at TIMESTAMP NULL,
    review_comments VARCHAR(255) NULL,
    CONSTRAINT fk_leave_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (employee_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by) 
        REFERENCES users (user_id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 9. PAYROLLS TABLE (Monthly payroll master header)
CREATE TABLE IF NOT EXISTS payrolls (
    payroll_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    payroll_month INT NOT NULL CHECK (payroll_month BETWEEN 1 AND 12),
    payroll_year INT NOT NULL CHECK (payroll_year >= 2000),
    basic_salary DECIMAL(12, 2) NOT NULL,
    total_allowances DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    gross_salary DECIMAL(12, 2) NOT NULL,
    total_deductions DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    net_salary DECIMAL(12, 2) NOT NULL,
    working_days INT NOT NULL DEFAULT 30,
    present_days INT NOT NULL DEFAULT 30,
    unpaid_leave_days INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSED, PAID
    payment_date DATE NULL,
    payment_method VARCHAR(30) NULL, -- BANK_TRANSFER, CHEQUE, CASH
    payment_reference VARCHAR(100) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_employee FOREIGN KEY (employee_id) 
        REFERENCES employees (employee_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_emp_month_year UNIQUE (employee_id, payroll_month, payroll_year)
);

-- 10. PAYROLL DETAILS TABLE (Line-item breakdown of allowances & deductions per payroll)
CREATE TABLE IF NOT EXISTS payroll_details (
    detail_id INT AUTO_INCREMENT PRIMARY KEY,
    payroll_id INT NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    component_type VARCHAR(20) NOT NULL, -- EARNING, DEDUCTION
    amount DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_pd_payroll FOREIGN KEY (payroll_id) 
        REFERENCES payrolls (payroll_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create Indexes for fast query lookups and search filtering
CREATE INDEX idx_emp_dept ON employees(department_id);
CREATE INDEX idx_emp_status ON employees(employment_status);
CREATE INDEX idx_att_emp_date ON attendance(employee_id, attendance_date);
CREATE INDEX idx_leave_emp_status ON leave_requests(employee_id, status);
CREATE INDEX idx_pay_emp_period ON payrolls(employee_id, payroll_month, payroll_year);
CREATE INDEX idx_pay_status ON payrolls(status);
