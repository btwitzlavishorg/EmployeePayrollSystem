/**
 * Payroll Processing Controller.
 * The core financial calculation engine interface.
 * Connects directly to Java PayrollCalculationService and enforces duplicate checks.
 */
const PayrollView = {
  employees: [],
  selectedEmployeeId: null,
  draftPayroll: null,
  processedPayrolls: [],

  async init() {
    this.setupDateSelectors();
    await this.loadEmployees();
    this.setupListeners();
    await this.loadPeriodPayrolls();
  },

  setupDateSelectors() {
    const monthSelect = document.getElementById('payroll-select-month');
    const yearSelect = document.getElementById('payroll-select-year');
    const filterMonth = document.getElementById('payroll-filter-month');
    const filterYear = document.getElementById('payroll-filter-year');

    const now = new Date();
    const curMonth = now.getMonth() + 1;
    const curYear = now.getFullYear();

    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const monthOpts = months.map((m, i) => `<option value="${i + 1}" ${i + 1 === curMonth ? 'selected' : ''}>${m}</option>`).join('');
    const yearOpts = [curYear - 1, curYear, curYear + 1].map(y => `<option value="${y}" ${y === curYear ? 'selected' : ''}>${y}</option>`).join('');

    if (monthSelect) monthSelect.innerHTML = monthOpts;
    if (yearSelect) yearSelect.innerHTML = yearOpts;
    if (filterMonth) filterMonth.innerHTML = monthOpts;
    if (filterYear) filterYear.innerHTML = yearOpts;
  },

  async loadEmployees() {
    try {
      this.employees = await API.getEmployees({ status: 'ACTIVE' });
      const select = document.getElementById('payroll-select-emp');
      if (select) {
        select.innerHTML = `<option value="">-- Choose Employee --</option>` +
          this.employees.map(e => `<option value="${e.employeeId}">${e.employeeCode} - ${e.fullName} (${e.departmentName || 'General'})</option>`).join('');
      }
    } catch (err) {
      console.error('Failed to load active employees for payroll:', err);
    }
  },

  async onEmployeeOrPeriodChange() {
    const empSelect = document.getElementById('payroll-select-emp');
    const monthSelect = document.getElementById('payroll-select-month');
    const yearSelect = document.getElementById('payroll-select-year');
    const adjInput = document.getElementById('payroll-manual-adjustment');
    const reasonInput = document.getElementById('payroll-adjustment-reason');

    const empId = parseInt(empSelect?.value) || 0;
    const month = parseInt(monthSelect?.value) || 0;
    const year = parseInt(yearSelect?.value) || 0;
    const manualAdjustment = parseFloat(adjInput?.value) || 0;
    const adjustmentReason = reasonInput?.value.trim() || '';

    this.selectedEmployeeId = empId;

    if (!empId || !month || !year) {
      document.getElementById('payroll-draft-preview-container').style.display = 'none';
      document.getElementById('payroll-empty-prompt').style.display = 'block';
      return;
    }

    // Call Java Backend for authoritative draft calculation
    try {
      const res = await API.calculatePayrollPreview({
        employeeId: empId,
        payrollMonth: month,
        payrollYear: year,
        manualAdjustment: manualAdjustment,
        adjustmentReason: adjustmentReason
      });

      if (res && res.payroll) {
        this.draftPayroll = res.payroll;
        this.renderDraftPreview(res);
      }
    } catch (err) {
      App.showToast('Calculation error: ' + err.message, 'error');
    }
  },

  renderDraftPreview(previewRes) {
    const p = previewRes.payroll;
    const isDuplicate = previewRes.isDuplicate;

    document.getElementById('payroll-empty-prompt').style.display = 'none';
    document.getElementById('payroll-draft-preview-container').style.display = 'block';

    // Duplicate Warning Banner
    const dupAlert = document.getElementById('payroll-duplicate-alert');
    if (dupAlert) {
      if (isDuplicate) {
        dupAlert.style.display = 'flex';
        document.getElementById('payroll-dup-status').textContent = previewRes.existingStatus;
        document.getElementById('btn-process-payroll').disabled = true;
      } else {
        dupAlert.style.display = 'none';
        document.getElementById('btn-process-payroll').disabled = false;
      }
    }

    // Summary Metadata
    document.getElementById('draft-emp-code').textContent = p.employeeCode;
    document.getElementById('draft-emp-name').textContent = p.employeeName;
    document.getElementById('draft-emp-dept').textContent = p.departmentName;
    document.getElementById('draft-emp-desig').textContent = p.designation;
    document.getElementById('draft-emp-bank').textContent = `${p.bankName} (${p.bankAccountNo})`;
    document.getElementById('draft-emp-pan').textContent = p.panNo;

    // Attendance Metrics
    document.getElementById('draft-work-days').textContent = `${p.workingDays} Days`;
    document.getElementById('draft-present-days').textContent = `${p.presentDays} Days`;
    document.getElementById('draft-unpaid-days').textContent = `${p.unpaidLeaveDays} Days`;

    // Totals
    document.getElementById('draft-basic-salary').textContent = Validation.formatCurrency(p.basicSalary);
    document.getElementById('draft-total-allowances').textContent = `+ ${Validation.formatCurrency(p.totalAllowances)}`;
    document.getElementById('draft-gross-salary').textContent = Validation.formatCurrency(p.grossSalary);
    document.getElementById('draft-total-deductions').textContent = `- ${Validation.formatCurrency(p.totalDeductions)}`;
    document.getElementById('draft-net-salary').textContent = Validation.formatCurrency(p.netSalary);

    // Line-items table
    const detailsTbody = document.getElementById('draft-details-tbody');
    if (detailsTbody && p.details) {
      detailsTbody.innerHTML = p.details.map(d => {
        const isEarning = d.componentType === 'EARNING';
        return `
          <tr>
            <td><strong>${d.componentName}</strong></td>
            <td><span class="status-pill status-${isEarning ? 'present' : 'absent'}">${d.componentType}</span></td>
            <td style="text-align:right; font-weight:600; color:${isEarning ? 'var(--text-main)' : 'var(--danger)'};">
              ${isEarning ? '+' : '-'} ${Validation.formatCurrency(d.amount)}
            </td>
          </tr>
        `;
      }).join('');
    }
  },

  async executeProcessPayroll() {
    if (!this.selectedEmployeeId || !this.draftPayroll) {
      App.showToast('Please select an employee and calculate preview first.', 'warning');
      return;
    }

    const month = parseInt(document.getElementById('payroll-select-month').value);
    const year = parseInt(document.getElementById('payroll-select-year').value);
    const manualAdjustment = parseFloat(document.getElementById('payroll-manual-adjustment').value) || 0;
    const adjustmentReason = document.getElementById('payroll-adjustment-reason').value.trim();

    App.confirm(
      'Confirm Payroll Generation',
      `Generate and finalize payroll for ${this.draftPayroll.employeeName} for ${document.getElementById('payroll-select-month').selectedOptions[0].text} ${year}? Net Payout: ${Validation.formatCurrency(this.draftPayroll.netSalary)}`,
      async () => {
        try {
          const res = await API.processPayroll({
            employeeId: this.selectedEmployeeId,
            payrollMonth: month,
            payrollYear: year,
            manualAdjustment: manualAdjustment,
            adjustmentReason: adjustmentReason
          });

          App.showToast(res.message, 'success');
          // Refresh period table & preview
          await this.loadPeriodPayrolls();
          await this.onEmployeeOrPeriodChange();
        } catch (err) {
          App.showToast(err.message, 'error');
        }
      },
      'Generate Payroll'
    );
  },

  async openBatchProcessModal() {
    const curMonth = document.getElementById('payroll-filter-month').value;
    const curYear = document.getElementById('payroll-filter-year').value;
    const monthText = document.getElementById('payroll-filter-month').selectedOptions[0].text;

    App.confirm(
      'Run Monthly Batch Payroll',
      `Do you want to process payroll in batch for all active employees for ${monthText} ${curYear}? Employees with existing payroll records will be safely skipped.`,
      async () => {
        try {
          const res = await API.batchProcessPayroll(parseInt(curMonth), parseInt(curYear));
          App.showToast(res.message, 'success');
          await this.loadPeriodPayrolls();
        } catch (err) {
          App.showToast(err.message, 'error');
        }
      },
      'Run Batch Process'
    );
  },

  async loadPeriodPayrolls() {
    const m = document.getElementById('payroll-filter-month')?.value || (new Date().getMonth() + 1);
    const y = document.getElementById('payroll-filter-year')?.value || new Date().getFullYear();

    try {
      this.processedPayrolls = await API.getPayrolls(m, y);
      this.renderPayrollsTable();
    } catch (err) {
      console.error('Failed to load payroll records for period:', err);
    }
  },

  renderPayrollsTable() {
    const tbody = document.getElementById('payrolls-period-tbody');
    const countBadge = document.getElementById('payrolls-period-count');
    if (!tbody) return;

    if (countBadge) {
      countBadge.textContent = `${this.processedPayrolls.length} Processed Record(s)`;
    }

    if (!this.processedPayrolls || this.processedPayrolls.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="table-empty-state">No payroll records generated for this month.</td></tr>`;
      return;
    }

    tbody.innerHTML = this.processedPayrolls.map(p => {
      const isPaid = p.status === 'PAID';
      return `
        <tr>
          <td><strong>${p.employeeCode}</strong></td>
          <td>
            <div style="font-weight:600;">${p.employeeName}</div>
            <div style="font-size:0.75rem; color:var(--text-muted);">${p.departmentName || 'General'}</div>
          </td>
          <td>${p.presentDays} / ${p.workingDays} ${p.unpaidLeaveDays > 0 ? `<span style="color:var(--danger); font-size:0.75rem;">(${p.unpaidLeaveDays} LOP)</span>` : ''}</td>
          <td>${Validation.formatCurrency(p.grossSalary)}</td>
          <td>${Validation.formatCurrency(p.totalDeductions)}</td>
          <td><strong style="color:var(--success); font-size:0.95rem;">${Validation.formatCurrency(p.netSalary)}</strong></td>
          <td><span class="status-pill status-${p.status.toLowerCase()}"><span class="status-dot"></span>${p.status}</span></td>
          <td>
            <div style="display:flex; gap:0.35rem;">
              ${!isPaid ? `
                <button class="btn-custom btn-success-custom btn-sm-custom" onclick="PayrollView.openMarkPaidModal(${p.payrollId}, '${p.employeeName}', ${p.netSalary})">Mark Paid</button>
              ` : `
                <span style="font-size:0.75rem; color:var(--text-muted); display:inline-flex; align-items:center;">Paid: ${Validation.formatDate(p.paymentDate)}</span>
              `}
              <button class="btn-custom btn-secondary-custom btn-sm-custom" onclick="PayrollView.viewPayslip(${p.payrollId})">Payslip</button>
              <button class="btn-custom btn-danger-custom btn-sm-custom" onclick="PayrollView.confirmDeletePayroll(${p.payrollId}, '${p.employeeName}')" title="Delete">Del</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  },

  openMarkPaidModal(payrollId, empName, netAmount) {
    document.getElementById('pay-modal-id').value = payrollId;
    document.getElementById('pay-modal-emp').textContent = empName;
    document.getElementById('pay-modal-amount').textContent = Validation.formatCurrency(netAmount);
    document.getElementById('pay-modal-date').value = new Date().toISOString().split('T')[0];
    document.getElementById('pay-modal-ref').value = 'NEFT-' + Math.floor(10000000 + Math.random() * 90000000);

    App.openModal('modal-mark-paid');
  },

  async submitMarkPaid() {
    const payrollId = document.getElementById('pay-modal-id').value;
    const data = {
      paymentDate: document.getElementById('pay-modal-date').value,
      paymentMethod: document.getElementById('pay-modal-method').value,
      paymentReference: document.getElementById('pay-modal-ref').value.trim()
    };

    try {
      await API.markPayrollPaid(payrollId, data);
      App.showToast('Payroll marked as PAID successfully!', 'success');
      App.closeModal('modal-mark-paid');
      await this.loadPeriodPayrolls();
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  },

  confirmDeletePayroll(payrollId, empName) {
    App.confirm(
      'Delete Payroll Record',
      `Are you sure you want to delete the payroll record for "${empName}"?`,
      async () => {
        try {
          await API.deletePayroll(payrollId);
          App.showToast('Payroll record deleted', 'success');
          await this.loadPeriodPayrolls();
          if (this.selectedEmployeeId) {
            await this.onEmployeeOrPeriodChange();
          }
        } catch (err) {
          App.showToast(err.message, 'error');
        }
      },
      'Delete Record',
      true
    );
  },

  viewPayslip(payrollId) {
    window.location.hash = 'payslips';
    setTimeout(() => {
      PayslipsView.loadSpecificPayroll(payrollId);
    }, 150);
  },

  setupListeners() {
    const empSelect = document.getElementById('payroll-select-emp');
    const monthSelect = document.getElementById('payroll-select-month');
    const yearSelect = document.getElementById('payroll-select-year');
    const adjInput = document.getElementById('payroll-manual-adjustment');

    if (empSelect) empSelect.addEventListener('change', () => this.onEmployeeOrPeriodChange());
    if (monthSelect) monthSelect.addEventListener('change', () => this.onEmployeeOrPeriodChange());
    if (yearSelect) yearSelect.addEventListener('change', () => this.onEmployeeOrPeriodChange());
    if (adjInput) adjInput.addEventListener('input', () => this.onEmployeeOrPeriodChange());

    const btnProcess = document.getElementById('btn-process-payroll');
    if (btnProcess) {
      btnProcess.addEventListener('click', () => this.executeProcessPayroll());
    }

    const btnBatch = document.getElementById('btn-batch-payroll');
    if (btnBatch) {
      btnBatch.addEventListener('click', () => this.openBatchProcessModal());
    }

    const filterMonth = document.getElementById('payroll-filter-month');
    const filterYear = document.getElementById('payroll-filter-year');
    if (filterMonth) filterMonth.addEventListener('change', () => this.loadPeriodPayrolls());
    if (filterYear) filterYear.addEventListener('change', () => this.loadPeriodPayrolls());

    const btnSubmitPaid = document.getElementById('btn-submit-mark-paid');
    if (btnSubmitPaid) {
      btnSubmitPaid.addEventListener('click', () => this.submitMarkPaid());
    }
  }
};
