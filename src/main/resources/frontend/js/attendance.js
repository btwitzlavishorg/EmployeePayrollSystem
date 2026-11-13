/**
 * Attendance & Leave Management Controller.
 * Manages daily attendance logs, leave request applications,
 * approvals/rejections, and LOP impacts.
 */
const AttendanceView = {
  attendanceList: [],
  leaveList: [],
  employees: [],
  activeTab: 'attendance',

  async init() {
    this.setupTabs();
    this.setupDatePickers();
    await this.loadEmployees();
    await this.loadAttendance();
    await this.loadLeaves();
    this.setupListeners();
  },

  setupTabs() {
    document.querySelectorAll('.tab-btn[data-tab]').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn[data-tab]').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        const tab = btn.getAttribute('data-tab');
        this.activeTab = tab;

        document.getElementById('tab-attendance-content').style.display = tab === 'attendance' ? 'block' : 'none';
        document.getElementById('tab-leaves-content').style.display = tab === 'leaves' ? 'block' : 'none';
      });
    });
  },

  setupDatePickers() {
    const filterDate = document.getElementById('attendance-filter-date');
    if (filterDate && !filterDate.value) {
      filterDate.value = new Date().toISOString().split('T')[0];
    }
  },

  async loadEmployees() {
    try {
      this.employees = await API.getEmployees();
      const attEmpSelect = document.getElementById('att-form-emp');
      const leaveEmpSelect = document.getElementById('leave-form-emp');

      const options = `<option value="">Select Employee</option>` +
        this.employees.map(e => `<option value="${e.employeeId}">${e.employeeCode} - ${e.fullName}</option>`).join('');

      if (attEmpSelect) attEmpSelect.innerHTML = options;
      if (leaveEmpSelect) leaveEmpSelect.innerHTML = options;
    } catch (err) {
      console.error('Failed to load employees for attendance:', err);
    }
  },

  async loadAttendance() {
    const dateInput = document.getElementById('attendance-filter-date');
    const date = dateInput ? dateInput.value : new Date().toISOString().split('T')[0];

    try {
      this.attendanceList = await API.getAttendance(date);
      this.renderAttendanceTable();
    } catch (err) {
      App.showToast('Failed to load attendance records: ' + err.message, 'error');
    }
  },

  renderAttendanceTable() {
    const tbody = document.getElementById('attendance-tbody');
    const countBadge = document.getElementById('attendance-count-badge');
    if (!tbody) return;

    if (countBadge) {
      countBadge.textContent = `${this.attendanceList.length} Logged Record(s)`;
    }

    if (!this.attendanceList || this.attendanceList.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="table-empty-state">No attendance records logged for this date.</td></tr>`;
      return;
    }

    tbody.innerHTML = this.attendanceList.map(a => `
      <tr>
        <td><strong>${a.employeeCode}</strong></td>
        <td>${a.employeeName}</td>
        <td>${Validation.formatDate(a.attendanceDate)}</td>
        <td><span class="status-pill status-${a.status.toLowerCase()}"><span class="status-dot"></span>${a.status}</span></td>
        <td>${Validation.formatTime(a.checkInTime)}</td>
        <td>${Validation.formatTime(a.checkOutTime)}</td>
        <td style="color:var(--text-muted);">${a.notes || '--'}</td>
      </tr>
    `).join('');
  },

  async loadLeaves() {
    try {
      this.leaveList = await API.getLeaves();
      this.renderLeavesTable();
    } catch (err) {
      App.showToast('Failed to load leave requests: ' + err.message, 'error');
    }
  },

  renderLeavesTable() {
    const tbody = document.getElementById('leaves-tbody');
    const countBadge = document.getElementById('leaves-count-badge');
    if (!tbody) return;

    if (countBadge) {
      const pendingCount = this.leaveList.filter(l => l.status === 'PENDING').length;
      countBadge.textContent = `${pendingCount} Pending Request(s)`;
    }

    if (!this.leaveList || this.leaveList.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="table-empty-state">No leave requests found.</td></tr>`;
      return;
    }

    tbody.innerHTML = this.leaveList.map(l => {
      const isPending = l.status === 'PENDING';
      const isUnpaid = l.leaveType === 'UNPAID';
      return `
        <tr>
          <td><strong>${l.employeeCode}</strong></td>
          <td>${l.employeeName}</td>
          <td>
            <span class="status-pill status-${isUnpaid ? 'absent' : 'on_leave'}">
              ${l.leaveType} ${isUnpaid ? '(LOP)' : ''}
            </span>
          </td>
          <td>${Validation.formatDate(l.startDate)} &rarr; ${Validation.formatDate(l.endDate)}</td>
          <td><strong>${l.totalDays} Day(s)</strong></td>
          <td style="max-width:200px; white-space:normal; font-size:0.82rem;">${l.reason}</td>
          <td><span class="status-pill status-${l.status.toLowerCase()}"><span class="status-dot"></span>${l.status}</span></td>
          <td>
            ${isPending ? `
              <div style="display:flex; gap:0.35rem;">
                <button class="btn-custom btn-success-custom btn-sm-custom" onclick="AttendanceView.openReviewModal(${l.leaveId}, 'APPROVED')">Approve</button>
                <button class="btn-custom btn-danger-custom btn-sm-custom" onclick="AttendanceView.openReviewModal(${l.leaveId}, 'REJECTED')">Reject</button>
              </div>
            ` : `
              <span style="font-size:0.75rem; color:var(--text-muted);">
                By ${l.reviewerName || 'Admin'} ${l.reviewComments ? `("${l.reviewComments}")` : ''}
              </span>
            `}
          </td>
        </tr>
      `;
    }).join('');
  },

  setupListeners() {
    const filterDate = document.getElementById('attendance-filter-date');
    if (filterDate) {
      filterDate.addEventListener('change', () => this.loadAttendance());
    }

    const btnMarkAtt = document.getElementById('btn-mark-attendance');
    if (btnMarkAtt) {
      btnMarkAtt.addEventListener('click', () => this.openMarkAttendanceModal());
    }

    const btnSaveAtt = document.getElementById('btn-save-attendance');
    if (btnSaveAtt) {
      btnSaveAtt.addEventListener('click', () => this.saveAttendance());
    }

    const btnApplyLeave = document.getElementById('btn-apply-leave');
    if (btnApplyLeave) {
      btnApplyLeave.addEventListener('click', () => this.openApplyLeaveModal());
    }

    const btnSaveLeave = document.getElementById('btn-save-leave');
    if (btnSaveLeave) {
      btnSaveLeave.addEventListener('click', () => this.saveLeave());
    }

    const btnSubmitReview = document.getElementById('btn-submit-leave-review');
    if (btnSubmitReview) {
      btnSubmitReview.addEventListener('click', () => this.submitReview());
    }
  },

  openMarkAttendanceModal() {
    const form = document.getElementById('attendance-form');
    if (form) form.reset();
    document.getElementById('att-form-date').value = new Date().toISOString().split('T')[0];
    document.getElementById('att-form-checkin').value = '09:00';
    document.getElementById('att-form-checkout').value = '18:00';
    App.openModal('modal-attendance-form');
  },

  async saveAttendance() {
    const data = {
      employeeId: parseInt(document.getElementById('att-form-emp').value) || 0,
      attendanceDate: document.getElementById('att-form-date').value,
      status: document.getElementById('att-form-status').value,
      checkInTime: document.getElementById('att-form-checkin').value,
      checkOutTime: document.getElementById('att-form-checkout').value,
      notes: document.getElementById('att-form-notes').value.trim()
    };

    if (!data.employeeId) {
      App.showToast('Please select an employee.', 'warning');
      return;
    }
    if (!data.attendanceDate) {
      App.showToast('Please specify the attendance date.', 'warning');
      return;
    }

    try {
      await API.markAttendance(data);
      App.showToast('Attendance logged successfully!', 'success');
      App.closeModal('modal-attendance-form');
      this.loadAttendance();
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  },

  openApplyLeaveModal() {
    const form = document.getElementById('leave-form');
    if (form) form.reset();
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('leave-form-start').value = today;
    document.getElementById('leave-form-end').value = today;
    App.openModal('modal-leave-form');
  },

  async saveLeave() {
    const data = {
      employeeId: parseInt(document.getElementById('leave-form-emp').value) || 0,
      leaveType: document.getElementById('leave-form-type').value,
      startDate: document.getElementById('leave-form-start').value,
      endDate: document.getElementById('leave-form-end').value,
      reason: document.getElementById('leave-form-reason').value.trim()
    };

    if (!data.employeeId) {
      App.showToast('Please select an employee.', 'warning');
      return;
    }
    if (!data.startDate || !data.endDate) {
      App.showToast('Start date and End date are required.', 'warning');
      return;
    }
    if (new Date(data.endDate) < new Date(data.startDate)) {
      App.showToast('End date cannot be earlier than start date.', 'warning');
      return;
    }
    if (!data.reason) {
      App.showToast('Please provide a reason for the leave.', 'warning');
      return;
    }

    try {
      await API.applyLeave(data);
      App.showToast('Leave request submitted successfully!', 'success');
      App.closeModal('modal-leave-form');
      this.loadLeaves();
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  },

  openReviewModal(leaveId, status) {
    document.getElementById('review-leave-id').value = leaveId;
    document.getElementById('review-leave-status').value = status;
    document.getElementById('review-leave-status-badge').textContent = status;
    document.getElementById('review-leave-status-badge').className = `status-pill status-${status.toLowerCase()}`;
    document.getElementById('review-leave-comments').value = status === 'APPROVED' ? 'Approved by HR.' : 'Rejected.';

    App.openModal('modal-leave-review');
  },

  async submitReview() {
    const leaveId = document.getElementById('review-leave-id').value;
    const status = document.getElementById('review-leave-status').value;
    const comments = document.getElementById('review-leave-comments').value.trim();

    try {
      await API.reviewLeave(leaveId, status, comments);
      App.showToast(`Leave request ${status.toLowerCase()} successfully!`, 'success');
      App.closeModal('modal-leave-review');
      this.loadLeaves();
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  }
};
