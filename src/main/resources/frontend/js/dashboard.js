/**
 * Dashboard Controller.
 * Loads live statistical metrics, analytical Chart.js graphs,
 * recent payroll transactions, and newly hired employees from MySQL database.
 */
const DashboardView = {
  trendChart: null,
  deptChart: null,

  async init() {
    await this.loadStats();
    this.setupQuickActions();
  },

  async loadStats() {
    try {
      const data = await API.getDashboardStats();
      if (!data) return;

      // 1. Statistics Cards
      document.getElementById('stat-total-emp').textContent = data.totalEmployees || 0;
      document.getElementById('stat-active-emp').textContent = data.activeEmployees || 0;
      document.getElementById('stat-active-sub').textContent = `${data.onLeaveEmployees || 0} currently on leave`;

      document.getElementById('stat-pending-payroll').textContent = data.pendingPayrolls || 0;
      document.getElementById('stat-pending-sub').textContent = `${data.pendingLeaveRequests || 0} pending leave request(s)`;

      document.getElementById('stat-monthly-payroll').textContent = Validation.formatCurrency(data.totalMonthlyPayroll);

      // Notification badge on topbar
      const pendingCount = (data.pendingPayrolls || 0) + (data.pendingLeaveRequests || 0);
      const pulse = document.querySelector('.notification-pulse');
      if (pulse) {
        pulse.style.display = pendingCount > 0 ? 'block' : 'none';
      }

      // 2. Charts
      this.renderTrendChart(data.monthlyTrend || []);
      this.renderDepartmentChart(data.departmentDistribution || {});

      // 3. Recent Payrolls Table
      this.renderRecentPayrolls(data.recentPayrolls || []);

      // 4. Recent Employees List
      this.renderRecentEmployees(data.recentEmployees || []);

    } catch (err) {
      console.error('Failed to load dashboard statistics:', err);
      App.showToast('Could not load dashboard statistics from backend', 'error');
    }
  },

  renderTrendChart(trendData) {
    const canvas = document.getElementById('chart-payroll-trend');
    if (!canvas) return;

    const labels = trendData.map(item => item.month);
    const values = trendData.map(item => item.totalPayroll);

    // If Chart.js is loaded
    if (typeof Chart !== 'undefined') {
      if (this.trendChart) this.trendChart.destroy();

      const ctx = canvas.getContext('2d');
      this.trendChart = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Monthly Net Payroll (₹)',
            data: values,
            backgroundColor: 'rgba(37, 99, 235, 0.85)',
            borderColor: '#2563eb',
            borderWidth: 1.5,
            borderRadius: 6,
            maxBarThickness: 45
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: (ctx) => ` Payout: ${Validation.formatCurrency(ctx.raw)}`
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              grid: { color: '#f1f5f9' },
              ticks: {
                callback: (val) => '₹ ' + (val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val)
              }
            },
            x: {
              grid: { display: false }
            }
          }
        }
      });
    } else {
      // Offline Pure Canvas Fallback
      this.renderCanvasBarFallback(canvas, labels, values);
    }
  },

  renderDepartmentChart(deptMap) {
    const canvas = document.getElementById('chart-dept-dist');
    if (!canvas) return;

    const labels = Object.keys(deptMap);
    const values = Object.values(deptMap);
    const colors = ['#2563eb', '#059669', '#d97706', '#7c3aed', '#0284c7', '#dc2626'];

    if (typeof Chart !== 'undefined') {
      if (this.deptChart) this.deptChart.destroy();

      const ctx = canvas.getContext('2d');
      this.deptChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: labels,
          datasets: [{
            data: values,
            backgroundColor: colors.slice(0, labels.length),
            borderWidth: 2,
            borderColor: '#ffffff'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: { boxWidth: 12, font: { size: 11 } }
            }
          },
          cutout: '68%'
        }
      });
    } else {
      this.renderCanvasDoughnutFallback(canvas, labels, values, colors);
    }
  },

  renderRecentPayrolls(payrolls) {
    const tbody = document.getElementById('dash-recent-payrolls-tbody');
    if (!tbody) return;

    if (!payrolls || payrolls.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="table-empty-state">No recent payroll transactions found.</td></tr>`;
      return;
    }

    tbody.innerHTML = payrolls.map(p => `
      <tr>
        <td><strong>${p.employeeCode}</strong></td>
        <td>${p.employeeName}</td>
        <td>${p.departmentName || 'General'}</td>
        <td>${Validation.formatCurrency(p.grossSalary)}</td>
        <td><strong>${Validation.formatCurrency(p.netSalary)}</strong></td>
        <td><span class="status-pill status-${p.status.toLowerCase()}"><span class="status-dot"></span>${p.status}</span></td>
      </tr>
    `).join('');
  },

  renderRecentEmployees(employees) {
    const container = document.getElementById('dash-recent-employees-list');
    if (!container) return;

    if (!employees || employees.length === 0) {
      container.innerHTML = `<div class="table-empty-state">No employees found.</div>`;
      return;
    }

    container.innerHTML = employees.map(e => {
      const initials = (e.firstName.charAt(0) + (e.lastName ? e.lastName.charAt(0) : '')).toUpperCase();
      return `
        <div class="recent-employee-item">
          <div style="display:flex; align-items:center;">
            <div class="emp-avatar-circle">${initials}</div>
            <div class="emp-details-mini">
              <span class="emp-name">${e.fullName}</span>
              <span class="emp-sub">${e.designation} &bull; ${e.departmentName || 'N/A'}</span>
            </div>
          </div>
          <span class="status-pill status-${e.employmentStatus.toLowerCase()}">${e.employmentStatus}</span>
        </div>
      `;
    }).join('');
  },

  setupQuickActions() {
    document.querySelectorAll('[data-quick-action]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const action = btn.getAttribute('data-quick-action');
        switch (action) {
          case 'add-employee':
            window.location.hash = 'employees';
            setTimeout(() => EmployeesView.openAddEmployeeModal(), 150);
            break;
          case 'process-payroll':
            window.location.hash = 'payroll';
            break;
          case 'mark-attendance':
            window.location.hash = 'attendance';
            setTimeout(() => AttendanceView.openMarkAttendanceModal(), 150);
            break;
          case 'view-reports':
            window.location.hash = 'reports';
            break;
        }
      });
    });
  },

  // Fallback pure canvas renderers in case CDN is unreachable in sandbox
  renderCanvasBarFallback(canvas, labels, values) {
    const ctx = canvas.getContext('2d');
    const w = canvas.width = canvas.parentElement.clientWidth;
    const h = canvas.height = canvas.parentElement.clientHeight || 260;
    ctx.clearRect(0, 0, w, h);

    const max = Math.max(...values, 1000);
    const pad = 40;
    const chartW = w - pad * 2;
    const chartH = h - pad * 2;
    const barW = Math.min(40, chartW / (labels.length || 1) - 15);

    // Grid lines
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
      const y = pad + (chartH / 4) * i;
      ctx.beginPath();
      ctx.moveTo(pad, y);
      ctx.lineTo(w - pad, y);
      ctx.stroke();
    }

    // Bars
    labels.forEach((lbl, i) => {
      const val = values[i] || 0;
      const barH = (val / max) * chartH;
      const x = pad + i * (chartW / labels.length) + (chartW / labels.length - barW) / 2;
      const y = pad + chartH - barH;

      ctx.fillStyle = '#2563eb';
      ctx.fillRect(x, y, barW, barH);

      ctx.fillStyle = '#475569';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(lbl, x + barW / 2, h - 15);
    });
  },

  renderCanvasDoughnutFallback(canvas, labels, values, colors) {
    const ctx = canvas.getContext('2d');
    const w = canvas.width = canvas.parentElement.clientWidth;
    const h = canvas.height = canvas.parentElement.clientHeight || 260;
    ctx.clearRect(0, 0, w, h);

    const total = values.reduce((a, b) => a + b, 0) || 1;
    const cx = w / 2;
    const cy = h / 2 - 15;
    const r = Math.min(cx, cy) - 20;
    let currentAngle = -Math.PI / 2;

    values.forEach((v, i) => {
      const sliceAngle = (v / total) * 2 * Math.PI;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, r, currentAngle, currentAngle + sliceAngle);
      ctx.closePath();
      ctx.fillStyle = colors[i % colors.length];
      ctx.fill();
      currentAngle += sliceAngle;
    });

    // Donut hole
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.65, 0, 2 * Math.PI);
    ctx.fillStyle = '#ffffff';
    ctx.fill();
  }
};
