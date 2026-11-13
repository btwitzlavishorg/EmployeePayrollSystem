/**
 * Salary Structure & Global Components Controller.
 * Manages compensation packages, global allowance/deduction catalogs,
 * and live authoritative salary breakdown calculations from Java backend.
 */
const SalaryView = {
  globalComponents: [],
  employees: [],
  selectedEmployeeId: null,
  currentStructure: null,

  async init() {
    await this.loadEmployees();
    await this.loadGlobalComponents();
    this.setupListeners();
  },

  async loadEmployees() {
    try {
      this.employees = await API.getEmployees();
      const select = document.getElementById('salary-select-emp');
      if (select) {
        select.innerHTML = `<option value="">-- Choose Employee to Configure --</option>` +
          this.employees.map(e => `<option value="${e.employeeId}">${e.employeeCode} - ${e.fullName} (${e.departmentName || 'General'})</option>`).join('');

        // Preselect first employee if none selected
        if (!this.selectedEmployeeId && this.employees.length > 0) {
          this.selectedEmployeeId = this.employees[0].employeeId;
          select.value = this.selectedEmployeeId;
          this.loadEmployeeSalaryStructure(this.selectedEmployeeId);
        }
      }
    } catch (err) {
      console.error('Failed to load employees for salary view:', err);
    }
  },

  async loadGlobalComponents() {
    try {
      this.globalComponents = await API.getSalaryComponents();
      this.renderGlobalComponentsTable();
    } catch (err) {
      console.error('Failed to load salary components:', err);
    }
  },

  renderGlobalComponentsTable() {
    const tbody = document.getElementById('salary-catalog-tbody');
    if (!tbody) return;

    if (!this.globalComponents || this.globalComponents.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="table-empty-state">No components defined in catalog.</td></tr>`;
      return;
    }

    tbody.innerHTML = this.globalComponents.map(c => `
      <tr>
        <td><strong>${c.componentCode}</strong></td>
        <td>${c.componentName}</td>
        <td><span class="status-pill status-${c.componentType === 'EARNING' ? 'present' : 'absent'}">${c.componentType}</span></td>
        <td>${c.calculationType === 'PERCENTAGE_OF_BASIC' ? '% of Basic' : 'Fixed Amount'}</td>
        <td><strong>${c.calculationType === 'PERCENTAGE_OF_BASIC' ? c.defaultValue + '%' : Validation.formatCurrency(c.defaultValue)}</strong></td>
        <td><span class="status-pill status-${c.active ? 'active' : 'terminated'}">${c.active ? 'Active' : 'Disabled'}</span></td>
      </tr>
    `).join('');
  },

  async loadEmployeeSalaryStructure(empId) {
    if (!empId) return;
    this.selectedEmployeeId = empId;

    try {
      this.currentStructure = await API.getEmployeeSalaryStructure(empId);
      this.renderStructureEditor();
    } catch (err) {
      App.showToast('Could not load salary structure: ' + err.message, 'error');
    }
  },

  renderStructureEditor() {
    const s = this.currentStructure;
    if (!s) return;

    document.getElementById('struct-emp-name').textContent = s.employeeName;
    document.getElementById('struct-emp-designation').textContent = `${s.designation} • ${s.departmentName || 'N/A'}`;
    document.getElementById('struct-basic-salary').value = s.basicSalary || 0;

    // Authoritative totals from backend
    document.getElementById('preview-basic').textContent = Validation.formatCurrency(s.basicSalary);
    document.getElementById('preview-allowances').textContent = `+ ${Validation.formatCurrency(s.totalAllowances)}`;
    document.getElementById('preview-gross').textContent = Validation.formatCurrency(s.grossSalary);
    document.getElementById('preview-deductions').textContent = `- ${Validation.formatCurrency(s.totalDeductions)}`;
    document.getElementById('preview-net').textContent = Validation.formatCurrency(s.netSalary);

    // Render Components Toggles
    const tbody = document.getElementById('struct-components-tbody');
    if (!tbody || !s.components) return;

    tbody.innerHTML = s.components.map((c, index) => {
      const isEarning = c.componentType === 'EARNING';
      const isPct = c.calculationType === 'PERCENTAGE_OF_BASIC';
      return `
        <tr data-comp-id="${c.componentId}">
          <td>
            <input type="checkbox" class="comp-toggle" data-index="${index}" ${c.enabled ? 'checked' : ''} style="cursor:pointer; width:16px; height:16px;">
          </td>
          <td>
            <strong>${c.componentName}</strong>
            <div style="font-size:0.75rem; color:var(--text-muted);">${c.componentCode} • ${c.description || ''}</div>
          </td>
          <td>
            <span class="status-pill status-${isEarning ? 'present' : 'absent'}">${c.componentType}</span>
          </td>
          <td>${isPct ? '% of Basic' : 'Fixed (₹)'}</td>
          <td>
            <div style="display:flex; align-items:center; gap:0.35rem;">
              <input type="number" step="0.01" class="form-control-custom comp-val-input" data-index="${index}"
                value="${c.customValue !== null && c.customValue !== undefined ? c.customValue : c.defaultValue}" 
                style="width:90px; padding:0.25rem 0.5rem; font-size:0.85rem;" ${!c.enabled ? 'disabled' : ''}>
              <span style="font-size:0.8rem; color:var(--text-secondary);">${isPct ? '%' : '₹'}</span>
            </div>
          </td>
          <td style="text-align:right;">
            <strong class="comp-calculated-amt" data-index="${index}">${Validation.formatCurrency(c.calculatedAmount)}</strong>
          </td>
        </tr>
      `;
    }).join('');

    this.attachComponentInputsListeners();
  },

  attachComponentInputsListeners() {
    // Checkbox toggles
    document.querySelectorAll('.comp-toggle').forEach(chk => {
      chk.addEventListener('change', (e) => {
        const idx = parseInt(e.target.getAttribute('data-index'));
        const isEnabled = e.target.checked;
        this.currentStructure.components[idx].enabled = isEnabled;

        const valInput = document.querySelector(`.comp-val-input[data-index="${idx}"]`);
        if (valInput) valInput.disabled = !isEnabled;

        this.recalculateClientPreview();
      });
    });

    // Custom value changes
    document.querySelectorAll('.comp-val-input').forEach(input => {
      input.addEventListener('input', (e) => {
        const idx = parseInt(e.target.getAttribute('data-index'));
        const val = parseFloat(e.target.value) || 0;
        this.currentStructure.components[idx].customValue = val;
        this.currentStructure.components[idx].effectiveValue = val;

        this.recalculateClientPreview();
      });
    });

    // Basic salary change
    const basicInput = document.getElementById('struct-basic-salary');
    if (basicInput) {
      basicInput.addEventListener('input', () => {
        const val = parseFloat(basicInput.value) || 0;
        this.currentStructure.basicSalary = val;
        this.recalculateClientPreview();
      });
    }
  },

  recalculateClientPreview() {
    const s = this.currentStructure;
    const basic = s.basicSalary || 0;
    let allowances = 0;
    let deductions = 0;

    s.components.forEach((c, idx) => {
      let amt = 0;
      if (c.enabled) {
        const rate = (c.customValue !== null && c.customValue !== undefined) ? c.customValue : c.defaultValue;
        if (c.calculationType === 'PERCENTAGE_OF_BASIC') {
          amt = (basic * rate) / 100;
        } else {
          amt = rate;
        }

        if (c.componentType === 'EARNING') allowances += amt;
        if (c.componentType === 'DEDUCTION') deductions += amt;
      }

      c.calculatedAmount = amt;
      const amtEl = document.querySelector(`.comp-calculated-amt[data-index="${idx}"]`);
      if (amtEl) amtEl.textContent = Validation.formatCurrency(amt);
    });

    const gross = basic + allowances;
    const net = Math.max(0, gross - deductions);

    document.getElementById('preview-basic').textContent = Validation.formatCurrency(basic);
    document.getElementById('preview-allowances').textContent = `+ ${Validation.formatCurrency(allowances)}`;
    document.getElementById('preview-gross').textContent = Validation.formatCurrency(gross);
    document.getElementById('preview-deductions').textContent = `- ${Validation.formatCurrency(deductions)}`;
    document.getElementById('preview-net').textContent = Validation.formatCurrency(net);
  },

  async saveStructure() {
    if (!this.selectedEmployeeId || !this.currentStructure) return;

    const basicSalary = parseFloat(document.getElementById('struct-basic-salary').value) || 0;
    if (basicSalary <= 0) {
      App.showToast('Please specify a valid basic salary greater than 0.', 'warning');
      return;
    }

    const payload = {
      basicSalary: basicSalary,
      components: this.currentStructure.components.map(c => ({
        componentId: c.componentId,
        enabled: !!c.enabled,
        customValue: (c.customValue !== null && c.customValue !== undefined) ? parseFloat(c.customValue) : null
      }))
    };

    try {
      await API.updateEmployeeSalaryStructure(this.selectedEmployeeId, payload);
      App.showToast('Salary structure updated successfully in Java database!', 'success');
      // Reload authoritative calculation directly from Java backend
      await this.loadEmployeeSalaryStructure(this.selectedEmployeeId);
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  },

  setupListeners() {
    const select = document.getElementById('salary-select-emp');
    if (select) {
      select.addEventListener('change', (e) => {
        this.loadEmployeeSalaryStructure(parseInt(e.target.value));
      });
    }

    const saveBtn = document.getElementById('btn-save-salary-structure');
    if (saveBtn) {
      saveBtn.addEventListener('click', () => this.saveStructure());
    }

    const addCompBtn = document.getElementById('btn-add-salary-comp');
    if (addCompBtn) {
      addCompBtn.addEventListener('click', () => {
        const form = document.getElementById('salary-comp-form');
        if (form) form.reset();
        App.openModal('modal-salary-comp-form');
      });
    }

    const saveCompBtn = document.getElementById('btn-save-salary-comp');
    if (saveCompBtn) {
      saveCompBtn.addEventListener('click', () => this.saveSalaryComponent());
    }
  },

  async saveSalaryComponent() {
    const data = {
      componentCode: document.getElementById('comp-form-code').value.trim().toUpperCase(),
      componentName: document.getElementById('comp-form-name').value.trim(),
      componentType: document.getElementById('comp-form-type').value,
      calculationType: document.getElementById('comp-form-calc-type').value,
      defaultValue: parseFloat(document.getElementById('comp-form-default-val').value) || 0,
      description: document.getElementById('comp-form-desc').value.trim(),
      active: true
    };

    if (!data.componentCode || !data.componentName) {
      App.showToast('Component Code and Name are required.', 'warning');
      return;
    }

    try {
      await API.createSalaryComponent(data);
      App.showToast('Salary component added to catalog successfully!', 'success');
      App.closeModal('modal-salary-comp-form');
      await this.loadGlobalComponents();
      if (this.selectedEmployeeId) {
        await this.loadEmployeeSalaryStructure(this.selectedEmployeeId);
      }
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  }
};
