/**
 * Employee Management Controller.
 * Handles employee listing, search, multi-field filtering, full CRUD modals,
 * and detailed view with salary breakdown.
 */
const EmployeesView = {
  employees: [],
  departments: [],

  async init() {
    await this.loadDepartments();
    await this.loadEmployees();
    this.setupListeners();
  },

  async loadDepartments() {
    try {
      this.departments = await API.getDepartments();
      this.populateDepartmentDropdowns();
    } catch (err) {
      console.error('Failed to load departments:', err);
    }
  },

  populateDepartmentDropdowns() {
    const filterSelect = document.getElementById('emp-filter-dept');
    const formSelect = document.getElementById('emp-form-dept');

    if (filterSelect) {
      filterSelect.innerHTML = `<option value="all">All Departments</option>` +
        this.departments.map(d => `<option value="${d.departmentId}">${d.departmentName}</option>`).join('');
    }

    if (formSelect) {
      formSelect.innerHTML = `<option value="">Select Department</option>` +
        this.departments.map(d => `<option value="${d.departmentId}">${d.departmentName}</option>`).join('');
    }
  },

  async loadEmployees() {
    const keyword = document.getElementById('emp-search-input')?.value.trim() || '';
    const deptId = document.getElementById('emp-filter-dept')?.value || 'all';
    const status = document.getElementById('emp-filter-status')?.value || 'all';

    const params = {};
    if (keyword) params.keyword = keyword;
    if (deptId !== 'all') params.departmentId = deptId;
    if (status !== 'all') params.status = status;

    try {
      this.employees = await API.getEmployees(params);
      this.renderTable();
    } catch (err) {
      App.showToast('Failed to load employees: ' + err.message, 'error');
    }
  },

  renderTable() {
    const tbody = document.getElementById('employees-tbody');
    const countBadge = document.getElementById('employees-count-badge');
    if (!tbody) return;

    if (countBadge) {
      countBadge.textContent = `${this.employees.length} Employee(s)`;
    }

    if (!this.employees || this.employees.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="table-empty-state">No matching employees found.</td></tr>`;
      return;
    }

    tbody.innerHTML = this.employees.map(emp => {
      const initials = (emp.firstName.charAt(0) + (emp.lastName ? emp.lastName.charAt(0) : '')).toUpperCase();
      return `
        <tr>
          <td><span class="dept-code-badge">${emp.employeeCode}</span></td>
          <td>
            <div style="display:flex; align-items:center; gap:0.65rem;">
              <div class="emp-avatar-circle" style="width:32px; height:32px; font-size:0.75rem;">${initials}</div>
              <div>
                <div style="font-weight:600; color:var(--text-main);">${emp.fullName}</div>
                <div style="font-size:0.75rem; color:var(--text-muted);">${emp.email}</div>
              </div>
            </div>
          </td>
          <td>${emp.departmentName || 'N/A'}</td>
          <td>${emp.designation}</td>
          <td><strong>${Validation.formatCurrency(emp.basicSalary)}</strong></td>
          <td><span class="status-pill status-${emp.employmentStatus.toLowerCase()}"><span class="status-dot"></span>${emp.employmentStatus}</span></td>
          <td>
            <div style="display:flex; gap:0.35rem;">
              <button class="btn-custom btn-secondary-custom btn-sm-custom" onclick="EmployeesView.viewDetails(${emp.employeeId})" title="View Details">View</button>
              <button class="btn-custom btn-secondary-custom btn-sm-custom" onclick="EmployeesView.openEditEmployeeModal(${emp.employeeId})" title="Edit">Edit</button>
              <button class="btn-custom btn-danger-custom btn-sm-custom" onclick="EmployeesView.confirmDelete(${emp.employeeId}, '${emp.fullName}')" title="Delete">Delete</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  },

  setupListeners() {
    const searchInput = document.getElementById('emp-search-input');
    const deptFilter = document.getElementById('emp-filter-dept');
    const statusFilter = document.getElementById('emp-filter-status');

    let debounceTimer;
    if (searchInput) {
      searchInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => this.loadEmployees(), 250);
      });
    }

    if (deptFilter) {
      deptFilter.addEventListener('change', () => this.loadEmployees());
    }

    if (statusFilter) {
      statusFilter.addEventListener('change', () => this.loadEmployees());
    }

    const addBtn = document.getElementById('btn-add-employee');
    if (addBtn) {
      addBtn.addEventListener('click', () => this.openAddEmployeeModal());
    }

    const saveBtn = document.getElementById('btn-save-employee');
    if (saveBtn) {
      saveBtn.addEventListener('click', () => this.saveEmployee());
    }
  },

  async openAddEmployeeModal() {
    const form = document.getElementById('employee-form');
    if (form) form.reset();
    document.getElementById('emp-form-id').value = '';
    document.getElementById('modal-employee-title').textContent = 'Add New Employee';

    // Auto-fetch next employee code from backend
    try {
      const codeRes = await API.getNextEmployeeCode();
      if (codeRes && codeRes.nextCode) {
        document.getElementById('emp-form-code').value = codeRes.nextCode;
      }
    } catch (ignored) {}

    // Set today as default joining date
    const dojInput = document.getElementById('emp-form-doj');
    if (dojInput) dojInput.value = new Date().toISOString().split('T')[0];

    App.openModal('modal-employee-form');
  },

  async openEditEmployeeModal(empId) {
    try {
      const emp = await API.getEmployee(empId);
      if (!emp) return;

      document.getElementById('modal-employee-title').textContent = 'Edit Employee';
      document.getElementById('emp-form-id').value = emp.employeeId;
      document.getElementById('emp-form-code').value = emp.employeeCode;
      document.getElementById('emp-form-first-name').value = emp.firstName;
      document.getElementById('emp-form-last-name').value = emp.lastName;
      document.getElementById('emp-form-email').value = emp.email;
      document.getElementById('emp-form-phone').value = emp.phone;
      document.getElementById('emp-form-dept').value = emp.departmentId;
      document.getElementById('emp-form-designation').value = emp.designation;
      document.getElementById('emp-form-doj').value = emp.dateOfJoining || '';
      document.getElementById('emp-form-status').value = emp.employmentStatus;
      document.getElementById('emp-form-basic-salary').value = emp.basicSalary || '';
      document.getElementById('emp-form-bank-name').value = emp.bankName || '';
      document.getElementById('emp-form-bank-account').value = emp.bankAccountNo || '';
      document.getElementById('emp-form-bank-ifsc').value = emp.bankIfsc || '';
      document.getElementById('emp-form-pan').value = emp.panNo || '';
      document.getElementById('emp-form-address').value = emp.address || '';

      App.openModal('modal-employee-form');
    } catch (err) {
      App.showToast('Could not load employee details: ' + err.message, 'error');
    }
  },

  async saveEmployee() {
    const empId = document.getElementById('emp-form-id').value;
    const isEdit = !!empId;

    const data = {
      employeeCode: document.getElementById('emp-form-code').value.trim(),
      firstName: document.getElementById('emp-form-first-name').value.trim(),
      lastName: document.getElementById('emp-form-last-name').value.trim(),
      email: document.getElementById('emp-form-email').value.trim(),
      phone: document.getElementById('emp-form-phone').value.trim(),
      departmentId: parseInt(document.getElementById('emp-form-dept').value) || 0,
      designation: document.getElementById('emp-form-designation').value.trim(),
      dateOfJoining: document.getElementById('emp-form-doj').value,
      employmentStatus: document.getElementById('emp-form-status').value,
      basicSalary: parseFloat(document.getElementById('emp-form-basic-salary').value) || 0,
      bankName: document.getElementById('emp-form-bank-name').value.trim(),
      bankAccountNo: document.getElementById('emp-form-bank-account').value.trim(),
      bankIfsc: document.getElementById('emp-form-bank-ifsc').value.trim().toUpperCase(),
      panNo: document.getElementById('emp-form-pan').value.trim().toUpperCase(),
      address: document.getElementById('emp-form-address').value.trim()
    };

    // Client-side Validation
    if (!data.firstName || !data.lastName) {
      App.showToast('First Name and Last Name are required.', 'warning');
      return;
    }
    if (!Validation.isValidEmail(data.email)) {
      App.showToast('Please enter a valid work email address.', 'warning');
      return;
    }
    if (!Validation.isValidPhone(data.phone)) {
      App.showToast('Please enter a valid 10-digit phone number.', 'warning');
      return;
    }
    if (!data.departmentId) {
      App.showToast('Please select a department.', 'warning');
      return;
    }
    if (!data.designation) {
      App.showToast('Designation is required.', 'warning');
      return;
    }
    if (!data.dateOfJoining) {
      App.showToast('Date of Joining is required.', 'warning');
      return;
    }
    if (!Validation.isPositiveNumber(data.basicSalary) || data.basicSalary <= 0) {
      App.showToast('Please specify a valid basic salary greater than 0.', 'warning');
      return;
    }
    if (!data.bankName || !data.bankAccountNo) {
      App.showToast('Bank Name and Account Number are required.', 'warning');
      return;
    }
    if (!Validation.isValidIfsc(data.bankIfsc)) {
      App.showToast('Please enter a valid 11-character Bank IFSC code (e.g. HDFC0001234).', 'warning');
      return;
    }
    if (!Validation.isValidPan(data.panNo)) {
      App.showToast('Please enter a valid 10-character PAN number (e.g. ABCDE1234F).', 'warning');
      return;
    }

    try {
      if (isEdit) {
        await API.updateEmployee(empId, data);
        App.showToast('Employee updated successfully!', 'success');
      } else {
        await API.createEmployee(data);
        App.showToast('Employee created successfully!', 'success');
      }

      App.closeModal('modal-employee-form');
      this.loadEmployees();
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  },

  confirmDelete(empId, empName) {
    App.confirm(
      'Delete Employee Record',
      `Are you sure you want to delete employee "${empName}"? This action will permanently remove their records, attendance, and salary history.`,
      async () => {
        try {
          await API.deleteEmployee(empId);
          App.showToast('Employee deleted successfully', 'success');
          this.loadEmployees();
        } catch (err) {
          App.showToast(err.message, 'error');
        }
      },
      'Delete Employee',
      true
    );
  },

  async viewDetails(empId) {
    try {
      const emp = await API.getEmployee(empId);
      const struct = await API.getEmployeeSalaryStructure(empId);

      document.getElementById('view-emp-code').textContent = emp.employeeCode;
      document.getElementById('view-emp-name').textContent = emp.fullName;
      document.getElementById('view-emp-designation').textContent = emp.designation;
      document.getElementById('view-emp-dept').textContent = emp.departmentName || 'N/A';
      document.getElementById('view-emp-email').textContent = emp.email;
      document.getElementById('view-emp-phone').textContent = emp.phone;
      document.getElementById('view-emp-doj').textContent = Validation.formatDate(emp.dateOfJoining);
      document.getElementById('view-emp-status').innerHTML = `<span class="status-pill status-${emp.employmentStatus.toLowerCase()}"><span class="status-dot"></span>${emp.employmentStatus}</span>`;
      document.getElementById('view-emp-bank').textContent = `${emp.bankName} (A/C: ${emp.bankAccountNo})`;
      document.getElementById('view-emp-ifsc').textContent = emp.bankIfsc;
      document.getElementById('view-emp-pan').textContent = emp.panNo;
      document.getElementById('view-emp-address').textContent = emp.address || 'N/A';

      // Salary Breakdown Table
      document.getElementById('view-emp-basic').textContent = Validation.formatCurrency(struct.basicSalary);
      document.getElementById('view-emp-gross').textContent = Validation.formatCurrency(struct.grossSalary);
      document.getElementById('view-emp-deductions').textContent = Validation.formatCurrency(struct.totalDeductions);
      document.getElementById('view-emp-net').textContent = Validation.formatCurrency(struct.netSalary);

      const compTbody = document.getElementById('view-emp-components-tbody');
      if (compTbody && struct.components) {
        compTbody.innerHTML = struct.components.filter(c => c.enabled).map(c => `
          <tr>
            <td>${c.componentName}</td>
            <td><span class="status-pill status-${c.componentType === 'EARNING' ? 'present' : 'absent'}">${c.componentType}</span></td>
            <td>${c.calculationType === 'PERCENTAGE_OF_BASIC' ? c.effectiveValue + '% of Basic' : 'Fixed'}</td>
            <td style="text-align:right;"><strong>${Validation.formatCurrency(c.calculatedAmount)}</strong></td>
          </tr>
        `).join('');
      }

      App.openModal('modal-employee-details');
    } catch (err) {
      App.showToast('Could not load employee profile: ' + err.message, 'error');
    }
  }
};
