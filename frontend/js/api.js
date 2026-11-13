/**
 * Enterprise API Client for Employee Payroll Management System.
 * Connects directly to Java backend REST API endpoints.
 */
const API = {
  baseUrl: '/api',

  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const defaultHeaders = {
      'Accept': 'application/json'
    };

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
      defaultHeaders['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }

    options.headers = { ...defaultHeaders, ...options.headers };
    options.credentials = 'same-origin';

    try {
      const response = await fetch(url, options);

      // Handle binary downloads (PDF / CSV)
      const contentType = response.headers.get('content-type') || '';
      if (contentType.includes('application/pdf') || contentType.includes('text/csv')) {
        if (!response.ok) {
          if (response.status === 401) {
            this.handleUnauthorized();
          }
          throw new Error(`Download failed with status ${response.status}`);
        }
        return await response.blob();
      }

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        if (response.status === 401 && endpoint !== '/auth/login' && endpoint !== '/auth/me') {
          this.handleUnauthorized();
        }
        const errorMsg = data.error || data.message || `Request failed with status ${response.status}`;
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`API Error on ${options.method || 'GET'} ${url}:`, err);
      throw err;
    }
  },

  handleUnauthorized() {
    if (typeof App !== 'undefined' && App.showLoginScreen) {
      App.showLoginScreen();
    }
  },

  // Auth & Session
  login(username, password) {
    return this.request('/auth/login', { method: 'POST', body: { username, password } });
  },
  logout() {
    return this.request('/auth/logout', { method: 'POST' });
  },
  getCurrentUser() {
    return this.request('/auth/me');
  },

  // Dashboard
  getDashboardStats() {
    return this.request('/dashboard/stats');
  },

  // Employees
  getEmployees(params = {}) {
    const query = new URLSearchParams(params).toString();
    return this.request(`/employees${query ? '?' + query : ''}`);
  },
  getEmployee(id) {
    return this.request(`/employees/${id}`);
  },
  getNextEmployeeCode() {
    return this.request('/employees/next-code');
  },
  createEmployee(employeeData) {
    return this.request('/employees', { method: 'POST', body: employeeData });
  },
  updateEmployee(id, employeeData) {
    return this.request(`/employees/${id}`, { method: 'PUT', body: employeeData });
  },
  deleteEmployee(id) {
    return this.request(`/employees/${id}`, { method: 'DELETE' });
  },

  // Departments
  getDepartments() {
    return this.request('/departments');
  },
  createDepartment(deptData) {
    return this.request('/departments', { method: 'POST', body: deptData });
  },
  updateDepartment(id, deptData) {
    return this.request(`/departments/${id}`, { method: 'PUT', body: deptData });
  },
  deleteDepartment(id) {
    return this.request(`/departments/${id}`, { method: 'DELETE' });
  },

  // Attendance & Leaves
  getAttendance(date) {
    return this.request(`/attendance?date=${date}`);
  },
  getEmployeeAttendance(employeeId, month, year) {
    return this.request(`/attendance/employee?employeeId=${employeeId}&month=${month}&year=${year}`);
  },
  markAttendance(data) {
    return this.request('/attendance/mark', { method: 'POST', body: data });
  },
  getLeaves() {
    return this.request('/leaves');
  },
  applyLeave(leaveData) {
    return this.request('/leaves/apply', { method: 'POST', body: leaveData });
  },
  reviewLeave(leaveId, status, comments) {
    return this.request(`/leaves/${leaveId}/review`, {
      method: 'POST',
      body: { status, comments }
    });
  },

  // Salary Structure & Components
  getSalaryComponents() {
    return this.request('/salary/components');
  },
  createSalaryComponent(componentData) {
    return this.request('/salary/components', { method: 'POST', body: componentData });
  },
  updateSalaryComponent(id, componentData) {
    return this.request(`/salary/components/${id}`, { method: 'PUT', body: componentData });
  },
  getEmployeeSalaryStructure(employeeId) {
    return this.request(`/salary/structure/${employeeId}`);
  },
  updateEmployeeSalaryStructure(employeeId, data) {
    return this.request(`/salary/structure/${employeeId}`, { method: 'PUT', body: data });
  },

  // Payroll Processing
  calculatePayrollPreview(previewData) {
    return this.request('/payroll/calculate-preview', { method: 'POST', body: previewData });
  },
  processPayroll(processData) {
    return this.request('/payroll/process', { method: 'POST', body: processData });
  },
  batchProcessPayroll(month, year) {
    return this.request('/payroll/batch', {
      method: 'POST',
      body: { payrollMonth: month, payrollYear: year }
    });
  },
  getPayrolls(month, year) {
    return this.request(`/payrolls?month=${month}&year=${year}`);
  },
  getPayrollById(id) {
    return this.request(`/payrolls/${id}`);
  },
  markPayrollPaid(id, payData) {
    return this.request(`/payrolls/${id}/pay`, { method: 'POST', body: payData });
  },
  deletePayroll(id) {
    return this.request(`/payrolls/${id}`, { method: 'DELETE' });
  },
  downloadPayslipPdf(payrollId) {
    return this.request(`/payrolls/${payrollId}/pdf`);
  },

  // Reports
  getMonthlyReport(month, year) {
    return this.request(`/reports/monthly?month=${month}&year=${year}`);
  },
  getAnnualReport(year) {
    return this.request(`/reports/annual?year=${year}`);
  },
  downloadReportCsv(type, month, year) {
    return this.request(`/reports/export/csv?type=${type}&month=${month}&year=${year}`);
  },
  downloadReportPdf(type, month, year) {
    return this.request(`/reports/export/pdf?type=${type}&month=${month}&year=${year}`);
  },

  // Settings
  getSettingsInfo() {
    return this.request('/settings/info');
  },
  changePassword(currentPassword, newPassword) {
    return this.request('/settings/password', {
      method: 'POST',
      body: { currentPassword, newPassword }
    });
  },

  // Helper to trigger browser file download from Blob
  downloadBlob(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }
};
