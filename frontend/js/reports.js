/**
 * Reporting & Analytics Dashboard Controller.
 * Visualizes annual and monthly payroll expenses, department disbursements,
 * and exports formal CSV and PDF statements via Java backend.
 */
const ReportsView = {
  activeType: 'monthly',
  reportChart: null,

  async init() {
    this.setupFilters();
    this.setupListeners();
    await this.loadReport();
  },

  setupFilters() {
    const monthSelect = document.getElementById('report-month');
    const yearSelect = document.getElementById('report-year');

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

  async loadReport() {
    const type = document.getElementById('report-type')?.value || 'monthly';
    const month = parseInt(document.getElementById('report-month')?.value) || (new Date().getMonth() + 1);
    const year = parseInt(document.getElementById('report-year')?.value) || new Date().getFullYear();

    this.activeType = type;
    const monthGroup = document.getElementById('report-month-group');
    if (monthGroup) {
      monthGroup.style.display = type === 'annual' ? 'none' : 'block';
    }

    try {
      if (type === 'annual') {
        const data = await API.getAnnualReport(year);
        this.renderAnnualReport(data);
      } else {
        const data = await API.getMonthlyReport(month, year);
        this.renderMonthlyReport(data);
      }
    } catch (err) {
      App.showToast('Could not load report: ' + err.message, 'error');
    }
  },

  renderMonthlyReport(data) {
    // Summary Cards
    document.getElementById('rep-stat-1-val').textContent = data.totalEmployees || 0;
    document.getElementById('rep-stat-1-lbl').textContent = 'Employees Paid';

    document.getElementById('rep-stat-2-val').textContent = Validation.formatCurrency(data.totalGross);
    document.getElementById('rep-stat-2-lbl').textContent = 'Gross Disbursed';

    document.getElementById('rep-stat-3-val').textContent = Validation.formatCurrency(data.totalDeductions);
    document.getElementById('rep-stat-3-lbl').textContent = 'Statutory Deductions';

    document.getElementById('rep-stat-4-val').textContent = Validation.formatCurrency(data.totalNet);
    document.getElementById('rep-stat-4-lbl').textContent = 'Total Net Salary';

    // Chart: Department breakdown
    const deptTotals = data.departmentTotals || {};
    const labels = Object.keys(deptTotals);
    const values = Object.values(deptTotals);
    this.renderChart('bar', labels, values, 'Department Net Salary (₹)');

    // Table
    const thead = document.getElementById('report-table-thead');
    const tbody = document.getElementById('report-table-tbody');

    if (thead) {
      thead.innerHTML = `
        <tr>
          <th>Emp Code</th>
          <th>Employee Name</th>
          <th>Department</th>
          <th>Work/Present Days</th>
          <th>Basic Salary</th>
          <th>Allowances</th>
          <th>Gross Salary</th>
          <th>Deductions</th>
          <th>Net Payout</th>
          <th>Status</th>
        </tr>
      `;
    }

    if (tbody) {
      const records = data.records || [];
      if (records.length === 0) {
        tbody.innerHTML = `<tr><td colspan="10" class="table-empty-state">No payroll records for this period.</td></tr>`;
        return;
      }

      tbody.innerHTML = records.map(p => `
        <tr>
          <td><strong>${p.employeeCode}</strong></td>
          <td>${p.employeeName}</td>
          <td>${p.departmentName || 'General'}</td>
          <td>${p.presentDays} / ${p.workingDays}</td>
          <td>${Validation.formatCurrency(p.basicSalary)}</td>
          <td>${Validation.formatCurrency(p.totalAllowances)}</td>
          <td>${Validation.formatCurrency(p.grossSalary)}</td>
          <td>${Validation.formatCurrency(p.totalDeductions)}</td>
          <td><strong style="color:var(--success);">${Validation.formatCurrency(p.netSalary)}</strong></td>
          <td><span class="status-pill status-${p.status.toLowerCase()}">${p.status}</span></td>
        </tr>
      `).join('');
    }
  },

  renderAnnualReport(data) {
    const months = data.months || [];
    let annualGross = 0;
    let annualDeductions = 0;
    let annualNet = 0;

    months.forEach(m => {
      annualGross += m.grossSalary;
      annualDeductions += m.totalDeductions;
      annualNet += m.netSalary;
    });

    // Summary Cards
    document.getElementById('rep-stat-1-val').textContent = data.totalRecords || 0;
    document.getElementById('rep-stat-1-lbl').textContent = 'Total Paychecks Issued';

    document.getElementById('rep-stat-2-val').textContent = Validation.formatCurrency(annualGross);
    document.getElementById('rep-stat-2-lbl').textContent = 'Annual Gross Expense';

    document.getElementById('rep-stat-3-val').textContent = Validation.formatCurrency(annualDeductions);
    document.getElementById('rep-stat-3-lbl').textContent = 'Total Tax & Deductions';

    document.getElementById('rep-stat-4-val').textContent = Validation.formatCurrency(annualNet);
    document.getElementById('rep-stat-4-lbl').textContent = 'Total Net Disbursed';

    // Chart: Month-by-month Trend
    const labels = months.map(m => m.monthName.substring(0, 3));
    const values = months.map(m => m.netSalary);
    this.renderChart('line', labels, values, 'Monthly Net Expenditure (₹)');

    // Table
    const thead = document.getElementById('report-table-thead');
    const tbody = document.getElementById('report-table-tbody');

    if (thead) {
      thead.innerHTML = `
        <tr>
          <th>Month</th>
          <th>Employees Paid</th>
          <th>Gross Amount</th>
          <th>Deductions / Taxes</th>
          <th>Net Payout</th>
        </tr>
      `;
    }

    if (tbody) {
      tbody.innerHTML = months.map(m => `
        <tr>
          <td><strong>${m.monthName}</strong></td>
          <td>${m.employeeCount}</td>
          <td>${Validation.formatCurrency(m.grossSalary)}</td>
          <td>${Validation.formatCurrency(m.totalDeductions)}</td>
          <td><strong style="color:var(--success);">${Validation.formatCurrency(m.netSalary)}</strong></td>
        </tr>
      `).join('');
    }
  },

  renderChart(type, labels, values, datasetLabel) {
    const canvas = document.getElementById('chart-report-visual');
    if (!canvas) return;

    if (typeof Chart !== 'undefined') {
      if (this.reportChart) this.reportChart.destroy();

      const ctx = canvas.getContext('2d');
      this.reportChart = new Chart(ctx, {
        type: type,
        data: {
          labels: labels,
          datasets: [{
            label: datasetLabel,
            data: values,
            backgroundColor: type === 'bar' ? 'rgba(37, 99, 235, 0.85)' : 'rgba(37, 99, 235, 0.15)',
            borderColor: '#2563eb',
            borderWidth: 2,
            tension: 0.35,
            fill: type === 'line'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            tooltip: {
              callbacks: {
                label: (ctx) => ` Payout: ${Validation.formatCurrency(ctx.raw)}`
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: (val) => '₹ ' + (val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val)
              }
            }
          }
        }
      });
    }
  },

  async exportCsv() {
    const type = document.getElementById('report-type').value;
    const month = document.getElementById('report-month').value;
    const year = document.getElementById('report-year').value;

    try {
      App.showToast('Generating official CSV statement from Java backend...', 'info');
      const blob = await API.downloadReportCsv(type, month, year);
      API.downloadBlob(blob, `payroll_report_${type}_${year}.csv`);
      App.showToast('CSV report downloaded successfully!', 'success');
    } catch (err) {
      App.showToast('Failed to export CSV: ' + err.message, 'error');
    }
  },

  async exportPdf() {
    const type = document.getElementById('report-type').value;
    const month = document.getElementById('report-month').value;
    const year = document.getElementById('report-year').value;

    try {
      App.showToast('Generating analytical PDF report from Java backend...', 'info');
      const blob = await API.downloadReportPdf(type, month, year);
      API.downloadBlob(blob, `payroll_report_${type}_${year}.pdf`);
      App.showToast('PDF report downloaded successfully!', 'success');
    } catch (err) {
      App.showToast('Failed to export PDF: ' + err.message, 'error');
    }
  },

  setupListeners() {
    const typeSelect = document.getElementById('report-type');
    const monthSelect = document.getElementById('report-month');
    const yearSelect = document.getElementById('report-year');

    if (typeSelect) typeSelect.addEventListener('change', () => this.loadReport());
    if (monthSelect) monthSelect.addEventListener('change', () => this.loadReport());
    if (yearSelect) yearSelect.addEventListener('change', () => this.loadReport());

    const btnCsv = document.getElementById('btn-export-csv');
    if (btnCsv) btnCsv.addEventListener('click', () => this.exportCsv());

    const btnPdf = document.getElementById('btn-export-pdf');
    if (btnPdf) btnPdf.addEventListener('click', () => this.exportPdf());
  }
};
