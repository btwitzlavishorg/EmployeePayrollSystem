/**
 * Payslip Generator & Viewer Controller.
 * Displays high-fidelity corporate payslips with on-screen preview,
 * browser printing, and server-side OpenPDF dynamic generation.
 */
const PayslipsView = {
  currentPayrollId: null,
  currentPayroll: null,
  payrollsList: [],

  async init() {
    this.setupMonthYearSelectors();
    await this.loadPayrollsList();
    this.setupListeners();
  },

  setupMonthYearSelectors() {
    const monthSelect = document.getElementById('payslip-filter-month');
    const yearSelect = document.getElementById('payslip-filter-year');

    const now = new Date();
    const curMonth = now.getMonth() + 1;
    const curYear = now.getFullYear();

    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];

    if (monthSelect) {
      monthSelect.innerHTML = months.map((m, i) => `<option value="${i + 1}" ${i + 1 === curMonth ? 'selected' : ''}>${m}</option>`).join('');
    }
    if (yearSelect) {
      yearSelect.innerHTML = [curYear - 1, curYear, curYear + 1].map(y => `<option value="${y}" ${y === curYear ? 'selected' : ''}>${y}</option>`).join('');
    }
  },

  async loadPayrollsList() {
    const m = document.getElementById('payslip-filter-month')?.value || (new Date().getMonth() + 1);
    const y = document.getElementById('payslip-filter-year')?.value || new Date().getFullYear();

    try {
      this.payrollsList = await API.getPayrolls(m, y);
      this.renderPayrollsSelector();

      if (this.payrollsList.length > 0) {
        // If a specific payroll was requested earlier, keep it, otherwise pick first
        if (!this.currentPayrollId || !this.payrollsList.find(p => p.payrollId === this.currentPayrollId)) {
          this.loadSpecificPayroll(this.payrollsList[0].payrollId);
        } else {
          this.loadSpecificPayroll(this.currentPayrollId);
        }
      } else {
        this.renderEmptyState();
      }
    } catch (err) {
      console.error('Failed to load payslips list:', err);
    }
  },

  renderPayrollsSelector() {
    const select = document.getElementById('payslip-select-record');
    if (!select) return;

    if (!this.payrollsList || this.payrollsList.length === 0) {
      select.innerHTML = `<option value="">No payroll records for this period</option>`;
      return;
    }

    select.innerHTML = this.payrollsList.map(p => `
      <option value="${p.payrollId}">
        ${p.employeeCode} - ${p.employeeName} (${p.departmentName || 'General'}) - ${Validation.formatCurrency(p.netSalary)}
      </option>
    `).join('');

    if (this.currentPayrollId) {
      select.value = this.currentPayrollId;
    }
  },

  renderEmptyState() {
    document.getElementById('payslip-empty-state').style.display = 'block';
    document.getElementById('payslip-rendered-sheet').style.display = 'none';
    document.getElementById('btn-download-payslip-pdf').disabled = true;
    document.getElementById('btn-print-payslip').disabled = true;
  },

  async loadSpecificPayroll(payrollId) {
    if (!payrollId) return;
    this.currentPayrollId = payrollId;

    const select = document.getElementById('payslip-select-record');
    if (select) select.value = payrollId;

    try {
      this.currentPayroll = await API.getPayrollById(payrollId);
      this.renderPayslipStatement(this.currentPayroll);
    } catch (err) {
      App.showToast('Could not load payslip: ' + err.message, 'error');
    }
  },

  renderPayslipStatement(p) {
    document.getElementById('payslip-empty-state').style.display = 'none';
    document.getElementById('payslip-rendered-sheet').style.display = 'block';
    document.getElementById('btn-download-payslip-pdf').disabled = false;
    document.getElementById('btn-print-payslip').disabled = false;

    // Period Banner
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const monthName = months[p.payrollMonth - 1] || '';
    document.getElementById('slip-period-banner').textContent = `SALARY PAYSLIP FOR ${monthName.toUpperCase()} ${p.payrollYear}`;

    // Metadata
    document.getElementById('slip-emp-code').textContent = p.employeeCode;
    document.getElementById('slip-emp-name').textContent = p.employeeName;
    document.getElementById('slip-emp-dept').textContent = p.departmentName;
    document.getElementById('slip-emp-desig').textContent = p.designation;
    document.getElementById('slip-emp-bank').textContent = p.bankName || 'HDFC Bank';
    document.getElementById('slip-emp-acct').textContent = p.bankAccountNo || 'N/A';
    document.getElementById('slip-emp-pan').textContent = p.panNo || 'N/A';
    document.getElementById('slip-emp-status').textContent = p.status;

    document.getElementById('slip-work-days').textContent = p.workingDays;
    document.getElementById('slip-present-days').textContent = p.presentDays;
    document.getElementById('slip-unpaid-days').textContent = `${p.unpaidLeaveDays} Days`;
    document.getElementById('slip-pay-date').textContent = Validation.formatDate(p.paymentDate);

    // Earnings & Deductions Tables
    const earnings = (p.details || []).filter(d => d.componentType === 'EARNING');
    const deductions = (p.details || []).filter(d => d.componentType === 'DEDUCTION');

    const earnTbody = document.getElementById('slip-earnings-tbody');
    const dedTbody = document.getElementById('slip-deductions-tbody');

    if (earnTbody) {
      earnTbody.innerHTML = `
        <tr>
          <td>Basic Salary</td>
          <td style="text-align:right;"><strong>${Validation.formatCurrency(p.basicSalary)}</strong></td>
        </tr>
      ` + earnings.map(e => `
        <tr>
          <td>${e.componentName}</td>
          <td style="text-align:right;">${Validation.formatCurrency(e.amount)}</td>
        </tr>
      `).join('');
    }

    if (dedTbody) {
      dedTbody.innerHTML = deductions.map(d => `
        <tr>
          <td>${d.componentName}</td>
          <td style="text-align:right;">${Validation.formatCurrency(d.amount)}</td>
        </tr>
      `).join('');
    }

    // Totals
    document.getElementById('slip-gross-salary').textContent = Validation.formatCurrency(p.grossSalary);
    document.getElementById('slip-total-deductions').textContent = Validation.formatCurrency(p.totalDeductions);
    document.getElementById('slip-net-salary').textContent = Validation.formatCurrency(p.netSalary);
    document.getElementById('slip-net-words').textContent = this.numberToWords(p.netSalary) + ' Rupees Only';
  },

  async downloadPdf() {
    if (!this.currentPayrollId) return;

    try {
      App.showToast('Generating official PDF payslip from Java backend...', 'info');
      const blob = await API.downloadPayslipPdf(this.currentPayrollId);
      const filename = `Payslip_${this.currentPayroll.employeeCode}_${this.currentPayroll.payrollMonth}_${this.currentPayroll.payrollYear}.pdf`;
      API.downloadBlob(blob, filename);
      App.showToast('Payslip downloaded successfully!', 'success');
    } catch (err) {
      App.showToast('Failed to download PDF: ' + err.message, 'error');
    }
  },

  printPayslip() {
    window.print();
  },

  setupListeners() {
    const filterMonth = document.getElementById('payslip-filter-month');
    const filterYear = document.getElementById('payslip-filter-year');
    const select = document.getElementById('payslip-select-record');

    if (filterMonth) filterMonth.addEventListener('change', () => this.loadPayrollsList());
    if (filterYear) filterYear.addEventListener('change', () => this.loadPayrollsList());
    if (select) {
      select.addEventListener('change', (e) => {
        this.loadSpecificPayroll(parseInt(e.target.value));
      });
    }

    const btnPdf = document.getElementById('btn-download-payslip-pdf');
    if (btnPdf) btnPdf.addEventListener('click', () => this.downloadPdf());

    const btnPrint = document.getElementById('btn-print-payslip');
    if (btnPrint) btnPrint.addEventListener('click', () => this.printPayslip());
  },

  // Client helper for number to English words
  numberToWords(num) {
    num = Math.floor(num || 0);
    if (num === 0) return 'Zero';
    const a = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten',
      'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
    const b = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

    function inWords(n) {
      if (n < 20) return a[n];
      if (n < 100) return b[Math.floor(n / 10)] + (n % 10 !== 0 ? ' ' + a[n % 10] : '');
      if (n < 1000) return a[Math.floor(n / 100)] + ' Hundred' + (n % 100 !== 0 ? ' and ' + inWords(n % 100) : '');
      if (n < 100000) return inWords(Math.floor(n / 1000)) + ' Thousand' + (n % 1000 !== 0 ? ' ' + inWords(n % 1000) : '');
      if (n < 10000000) return inWords(Math.floor(n / 100000)) + ' Lakh' + (n % 100000 !== 0 ? ' ' + inWords(n % 100000) : '');
      return inWords(Math.floor(n / 10000000)) + ' Crore' + (n % 10000000 !== 0 ? ' ' + inWords(n % 10000000) : '');
    }

    return inWords(num);
  }
};
