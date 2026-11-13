-- ====================================================================
-- Employee Payroll Management System - Realistic Seed Data
-- ====================================================================

USE payroll_db;

-- 1. USERS
-- Default admin credentials: username='admin', password='admin123'
-- Default HR credentials: username='hrmanager', password='admin123'
INSERT INTO users (username, password_hash, salt, full_name, email, role, status) VALUES
('admin', 'cbe97a33f604ffd48e703109c249325564276f817b9e305c81b64631d5042c9c', 'd8f8a7e4b2c1d3e5', 'System Administrator', 'admin@payrollcorp.com', 'ADMIN', 'ACTIVE'),
('hrmanager', 'cbe97a33f604ffd48e703109c249325564276f817b9e305c81b64631d5042c9c', 'd8f8a7e4b2c1d3e5', 'Neha Sharma', 'neha.sharma@payrollcorp.com', 'HR_MANAGER', 'ACTIVE');

-- 2. DEPARTMENTS
INSERT INTO departments (department_code, department_name, description) VALUES
('ENG', 'Software Engineering', 'Core product engineering, backend systems, and platform infrastructure'),
('HR', 'Human Resources', 'Talent acquisition, employee relations, learning & payroll compliance'),
('FIN', 'Finance & Accounts', 'Financial planning, accounting, tax compliance, and auditing'),
('MKT', 'Marketing & Sales', 'Brand strategy, enterprise sales, public relations, and customer outreach'),
('OPS', 'Operations & IT Support', 'Infrastructure management, IT helpdesk, facility maintenance, and logistics');

-- 3. SALARY COMPONENTS (Configurable Allowances & Deductions)
INSERT INTO salary_components (component_code, component_name, component_type, calculation_type, default_value, is_active, description) VALUES
('HRA', 'House Rent Allowance', 'EARNING', 'PERCENTAGE_OF_BASIC', 40.00, TRUE, 'Housing accommodation allowance calculated as 40% of Basic'),
('DA', 'Dearness Allowance', 'EARNING', 'PERCENTAGE_OF_BASIC', 15.00, TRUE, 'Cost of living adjustment calculated as 15% of Basic'),
('CONV', 'Conveyance Allowance', 'EARNING', 'FIXED_AMOUNT', 2500.00, TRUE, 'Fixed monthly commuter transportation allowance'),
('MED', 'Medical Allowance', 'EARNING', 'FIXED_AMOUNT', 1500.00, TRUE, 'Fixed monthly medical reimbursement allowance'),
('SPECIAL', 'Special Allowance', 'EARNING', 'FIXED_AMOUNT', 3000.00, TRUE, 'Discretionary performance & position allowance'),
('PF', 'Provident Fund (Employee)', 'DEDUCTION', 'PERCENTAGE_OF_BASIC', 12.00, TRUE, 'Mandatory retirement savings scheme (12% of Basic)'),
('PTAX', 'Professional Tax', 'DEDUCTION', 'FIXED_AMOUNT', 200.00, TRUE, 'State professional employment tax statutory deduction'),
('TDS', 'Income Tax (TDS)', 'DEDUCTION', 'PERCENTAGE_OF_BASIC', 5.00, TRUE, 'Tax deducted at source based on annual projected bracket'),
('INS', 'Group Health Insurance', 'DEDUCTION', 'FIXED_AMOUNT', 750.00, TRUE, 'Corporate family health and term life insurance premium');

-- 4. EMPLOYEES
INSERT INTO employees (employee_code, first_name, last_name, email, phone, address, department_id, designation, date_of_joining, employment_status, bank_name, bank_account_no, bank_ifsc, pan_no) VALUES
('EMP1001', 'Aarav', 'Patel', 'aarav.patel@payrollcorp.com', '9876543210', '42 Tech Park Avenue, Silicon Valley Road, Bengaluru', 1, 'Senior Software Engineer', '2022-03-15', 'ACTIVE', 'HDFC Bank', '50100439281723', 'HDFC0001234', 'ABCDE1234F'),
('EMP1002', 'Priya', 'Sundaram', 'priya.s@payrollcorp.com', '9876543211', '15 Green Valley Residency, Indiranagar, Bengaluru', 1, 'Lead Backend Architect', '2021-07-01', 'ACTIVE', 'State Bank of India', '30987123456789', 'SBIN0004567', 'FGHIJ5678K'),
('EMP1003', 'Rohan', 'Verma', 'rohan.verma@payrollcorp.com', '9876543212', '78 Heritage Enclave, Whitefield, Bengaluru', 2, 'Senior HR Generalist', '2023-01-10', 'ACTIVE', 'ICICI Bank', '00120156789012', 'ICIC0000012', 'KLMNO9012P'),
('EMP1004', 'Ananya', 'Deshmukh', 'ananya.d@payrollcorp.com', '9876543213', '104 Sunrise Boulevard, Koramangala, Bengaluru', 3, 'Senior Financial Analyst', '2022-09-20', 'ACTIVE', 'Axis Bank', '91201004567890', 'UTIB0000912', 'PQRST3456U'),
('EMP1005', 'Vikram', 'Mehta', 'vikram.mehta@payrollcorp.com', '9876543214', '23 Palm Grove, HSR Layout, Bengaluru', 4, 'Marketing Director', '2020-11-05', 'ACTIVE', 'HDFC Bank', '50100987654321', 'HDFC0001234', 'UVWXY7890Z'),
('EMP1006', 'Sneha', 'Kulkarni', 'sneha.k@payrollcorp.com', '9876543215', '56 Lakeview Heights, Bellandur, Bengaluru', 5, 'IT Systems Lead', '2023-04-18', 'ON_LEAVE', 'Kotak Mahindra Bank', '43210987654321', 'KKBK0000432', 'ZABCD1234E');

-- 5. SALARY STRUCTURES
INSERT INTO salary_structures (employee_id, basic_salary, effective_date) VALUES
(1, 65000.00, '2024-01-01'),
(2, 90000.00, '2024-01-01'),
(3, 45000.00, '2024-01-01'),
(4, 55000.00, '2024-01-01'),
(5, 80000.00, '2024-01-01'),
(6, 40000.00, '2024-01-01');

-- 6. EMPLOYEE SALARY COMPONENT MAPPINGS
-- Map standard components to all employees
INSERT INTO employee_salary_components (employee_id, component_id, custom_value, is_enabled) VALUES
-- EMP1001
(1, 1, NULL, TRUE), (1, 2, NULL, TRUE), (1, 3, NULL, TRUE), (1, 4, NULL, TRUE), (1, 5, 2000.00, TRUE),
(1, 6, NULL, TRUE), (1, 7, NULL, TRUE), (1, 8, NULL, TRUE), (1, 9, NULL, TRUE),
-- EMP1002
(2, 1, NULL, TRUE), (2, 2, NULL, TRUE), (2, 3, NULL, TRUE), (2, 4, NULL, TRUE), (2, 5, 5000.00, TRUE),
(2, 6, NULL, TRUE), (2, 7, NULL, TRUE), (2, 8, 8.00, TRUE), (2, 9, NULL, TRUE),
-- EMP1003
(3, 1, NULL, TRUE), (3, 2, NULL, TRUE), (3, 3, NULL, TRUE), (3, 4, NULL, TRUE),
(3, 6, NULL, TRUE), (3, 7, NULL, TRUE), (3, 8, NULL, TRUE), (3, 9, NULL, TRUE),
-- EMP1004
(4, 1, NULL, TRUE), (4, 2, NULL, TRUE), (4, 3, NULL, TRUE), (4, 4, NULL, TRUE),
(4, 6, NULL, TRUE), (4, 7, NULL, TRUE), (4, 8, NULL, TRUE), (4, 9, NULL, TRUE),
-- EMP1005
(5, 1, NULL, TRUE), (5, 2, NULL, TRUE), (5, 3, NULL, TRUE), (5, 4, NULL, TRUE), (5, 5, 4000.00, TRUE),
(5, 6, NULL, TRUE), (5, 7, NULL, TRUE), (5, 8, 7.00, TRUE), (5, 9, NULL, TRUE),
-- EMP1006
(6, 1, NULL, TRUE), (6, 2, NULL, TRUE), (6, 3, NULL, TRUE), (6, 4, NULL, TRUE),
(6, 6, NULL, TRUE), (6, 7, NULL, TRUE), (6, 8, NULL, TRUE), (6, 9, NULL, TRUE);

-- 7. ATTENDANCE SEED DATA (Recent records)
INSERT INTO attendance (employee_id, attendance_date, status, check_in_time, check_out_time, notes) VALUES
(1, '2026-11-01', 'PRESENT', '09:12:00', '18:15:00', 'On-time checkin'),
(1, '2026-11-02', 'PRESENT', '09:05:00', '18:20:00', 'On-time checkin'),
(1, '2026-11-03', 'PRESENT', '09:28:00', '18:30:00', 'On-time checkin'),
(1, '2026-11-04', 'PRESENT', '09:10:00', '18:10:00', 'On-time checkin'),
(1, '2026-11-05', 'PRESENT', '09:15:00', '18:45:00', 'Sprint release support'),
(2, '2026-11-01', 'PRESENT', '09:00:00', '18:00:00', 'On-time checkin'),
(2, '2026-11-02', 'PRESENT', '09:02:00', '18:05:00', 'On-time checkin'),
(3, '2026-11-01', 'PRESENT', '09:30:00', '18:30:00', 'On-time checkin'),
(4, '2026-11-01', 'PRESENT', '09:15:00', '18:15:00', 'On-time checkin'),
(5, '2026-11-01', 'PRESENT', '09:20:00', '18:25:00', 'On-time checkin'),
(6, '2026-11-01', 'LEAVE', NULL, NULL, 'Medical leave approved');

-- 8. LEAVE REQUESTS
INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, total_days, reason, status, reviewed_by, reviewed_at, review_comments) VALUES
(6, 'SICK', '2026-11-01', '2026-11-15', 14, 'Recovery from minor surgical procedure', 'APPROVED', 1, '2026-10-28 14:30:00', 'Approved. Medical certificate verified.'),
(1, 'CASUAL', '2026-11-20', '2026-11-21', 2, 'Family wedding ceremony attending in hometown', 'PENDING', NULL, NULL, NULL),
(3, 'UNPAID', '2026-10-15', '2026-10-16', 2, 'Extended personal travel beyond leave quota', 'APPROVED', 1, '2026-10-12 11:00:00', 'Approved with LOP deduction.');

-- 9. HISTORICAL PAYROLL RECORDS (Previous Month: October 2026)
-- EMP1001 (Aarav Patel): Basic=65,000 | Allowances=HRA(26,000)+DA(9,750)+CONV(2,500)+MED(1,500)+SPECIAL(2,000) = 41,750 | Gross = 106,750
-- Deductions: PF(7,800)+PTAX(200)+TDS(3,250)+INS(750) = 12,000 | Net = 94,750
INSERT INTO payrolls (payroll_id, employee_id, payroll_month, payroll_year, basic_salary, total_allowances, gross_salary, total_deductions, net_salary, working_days, present_days, unpaid_leave_days, status, payment_date, payment_method, payment_reference, notes) VALUES
(1, 1, 10, 2026, 65000.00, 41750.00, 106750.00, 12000.00, 94750.00, 30, 30, 0, 'PAID', '2026-10-31', 'BANK_TRANSFER', 'NEFT-HDFC-98234101', 'Regular monthly salary credited'),
(2, 2, 10, 2026, 90000.00, 58500.00, 148500.00, 19750.00, 128750.00, 30, 30, 0, 'PAID', '2026-10-31', 'BANK_TRANSFER', 'NEFT-SBIN-87239102', 'Regular monthly salary credited'),
(3, 3, 10, 2026, 45000.00, 28750.00, 73750.00, 11450.00, 62300.00, 30, 28, 2, 'PAID', '2026-10-31', 'BANK_TRANSFER', 'NEFT-ICIC-76342103', 'Includes 2 days LOP deduction'),
(4, 4, 10, 2026, 55000.00, 34250.00, 89250.00, 10450.00, 78800.00, 30, 30, 0, 'PAID', '2026-10-31', 'BANK_TRANSFER', 'NEFT-UTIB-65453104', 'Regular monthly salary credited'),
(5, 5, 10, 2026, 80000.00, 52000.00, 132000.00, 16350.00, 115650.00, 30, 30, 0, 'PAID', '2026-10-31', 'BANK_TRANSFER', 'NEFT-HDFC-54321105', 'Regular monthly salary credited');

-- 10. PAYROLL DETAILS (Breakdown for Payroll #1: Aarav Patel)
INSERT INTO payroll_details (payroll_id, component_name, component_type, amount) VALUES
(1, 'House Rent Allowance (HRA)', 'EARNING', 26000.00),
(1, 'Dearness Allowance (DA)', 'EARNING', 9750.00),
(1, 'Conveyance Allowance', 'EARNING', 2500.00),
(1, 'Medical Allowance', 'EARNING', 1500.00),
(1, 'Special Allowance', 'EARNING', 2000.00),
(1, 'Provident Fund (PF)', 'DEDUCTION', 7800.00),
(1, 'Professional Tax', 'DEDUCTION', 200.00),
(1, 'Income Tax (TDS)', 'DEDUCTION', 3250.00),
(1, 'Group Health Insurance', 'DEDUCTION', 750.00);

-- Payroll #2 details (Priya Sundaram)
INSERT INTO payroll_details (payroll_id, component_name, component_type, amount) VALUES
(2, 'House Rent Allowance (HRA)', 'EARNING', 36000.00),
(2, 'Dearness Allowance (DA)', 'EARNING', 13500.00),
(2, 'Conveyance Allowance', 'EARNING', 2500.00),
(2, 'Medical Allowance', 'EARNING', 1500.00),
(2, 'Special Allowance', 'EARNING', 5000.00),
(2, 'Provident Fund (PF)', 'DEDUCTION', 10800.00),
(2, 'Professional Tax', 'DEDUCTION', 200.00),
(2, 'Income Tax (TDS)', 'DEDUCTION', 8000.00),
(2, 'Group Health Insurance', 'DEDUCTION', 750.00);
