/**
 * Department Management Controller.
 * Handles department listing with employee counts, creation, editing, and deletion.
 */
const DepartmentsView = {
  departments: [],

  async init() {
    await this.loadDepartments();
    this.setupListeners();
  },

  async loadDepartments() {
    try {
      this.departments = await API.getDepartments();
      this.render();
    } catch (err) {
      App.showToast('Failed to load departments: ' + err.message, 'error');
    }
  },

  render() {
    const grid = document.getElementById('departments-grid');
    const countBadge = document.getElementById('departments-count-badge');
    if (!grid) return;

    if (countBadge) {
      countBadge.textContent = `${this.departments.length} Department(s)`;
    }

    if (!this.departments || this.departments.length === 0) {
      grid.innerHTML = `<div class="table-empty-state" style="grid-column: 1/-1;">No departments found.</div>`;
      return;
    }

    grid.innerHTML = this.departments.map(dept => `
      <div class="department-card">
        <div>
          <div class="dept-header">
            <span class="dept-code-badge">${dept.departmentCode}</span>
            <div style="display:flex; gap:0.25rem;">
              <button class="btn-custom btn-secondary-custom btn-sm-custom" onclick="DepartmentsView.openEditModal(${dept.departmentId})">Edit</button>
              <button class="btn-custom btn-danger-custom btn-sm-custom" onclick="DepartmentsView.confirmDelete(${dept.departmentId}, '${dept.departmentName}', ${dept.employeeCount})">Delete</button>
            </div>
          </div>
          <div class="dept-name">${dept.departmentName}</div>
          <div class="dept-desc">${dept.description || 'No description provided.'}</div>
        </div>
        <div class="dept-footer">
          <div class="dept-emp-count">
            <span style="font-size:1.1rem;">👥</span>
            <strong>${dept.employeeCount}</strong> active employee(s)
          </div>
          <button class="btn-custom btn-secondary-custom btn-sm-custom" onclick="DepartmentsView.filterEmployeesByDept(${dept.departmentId})">View Staff</button>
        </div>
      </div>
    `).join('');
  },

  setupListeners() {
    const addBtn = document.getElementById('btn-add-department');
    if (addBtn) {
      addBtn.addEventListener('click', () => this.openAddModal());
    }

    const saveBtn = document.getElementById('btn-save-department');
    if (saveBtn) {
      saveBtn.addEventListener('click', () => this.saveDepartment());
    }
  },

  openAddModal() {
    const form = document.getElementById('department-form');
    if (form) form.reset();
    document.getElementById('dept-form-id').value = '';
    document.getElementById('modal-dept-title').textContent = 'Add Department';
    App.openModal('modal-department-form');
  },

  openEditModal(deptId) {
    const dept = this.departments.find(d => d.departmentId === deptId);
    if (!dept) return;

    document.getElementById('modal-dept-title').textContent = 'Edit Department';
    document.getElementById('dept-form-id').value = dept.departmentId;
    document.getElementById('dept-form-code').value = dept.departmentCode;
    document.getElementById('dept-form-name').value = dept.departmentName;
    document.getElementById('dept-form-desc').value = dept.description || '';

    App.openModal('modal-department-form');
  },

  async saveDepartment() {
    const id = document.getElementById('dept-form-id').value;
    const isEdit = !!id;

    const data = {
      departmentCode: document.getElementById('dept-form-code').value.trim().toUpperCase(),
      departmentName: document.getElementById('dept-form-name').value.trim(),
      description: document.getElementById('dept-form-desc').value.trim()
    };

    if (!data.departmentCode || !data.departmentName) {
      App.showToast('Department Code and Name are required.', 'warning');
      return;
    }

    try {
      if (isEdit) {
        await API.updateDepartment(id, data);
        App.showToast('Department updated successfully', 'success');
      } else {
        await API.createDepartment(data);
        App.showToast('Department created successfully', 'success');
      }

      App.closeModal('modal-department-form');
      this.loadDepartments();
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  },

  confirmDelete(deptId, deptName, empCount) {
    if (empCount > 0) {
      App.showToast(`Cannot delete "${deptName}": ${empCount} employee(s) are assigned to it. Reassign employees first.`, 'warning');
      return;
    }

    App.confirm(
      'Delete Department',
      `Are you sure you want to delete department "${deptName}"?`,
      async () => {
        try {
          await API.deleteDepartment(deptId);
          App.showToast('Department deleted successfully', 'success');
          this.loadDepartments();
        } catch (err) {
          App.showToast(err.message, 'error');
        }
      },
      'Delete Department',
      true
    );
  },

  filterEmployeesByDept(deptId) {
    window.location.hash = 'employees';
    setTimeout(() => {
      const select = document.getElementById('emp-filter-dept');
      if (select) {
        select.value = deptId;
        EmployeesView.loadEmployees();
      }
    }, 150);
  }
};
