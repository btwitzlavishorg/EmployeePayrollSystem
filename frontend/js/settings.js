/**
 * System Settings & Application Info Controller.
 * Displays runtime diagnostics, database connection status,
 * corporate metadata, and security credentials management.
 */
const SettingsView = {
  async init() {
    await this.loadSettings();
    this.setupListeners();
  },

  async loadSettings() {
    try {
      const data = await API.getSettingsInfo();
      if (!data) return;

      document.getElementById('set-company-name').textContent = data.companyName || 'PayrollCorp';
      document.getElementById('set-company-email').textContent = data.companyEmail || 'payroll@payrollcorp.com';
      document.getElementById('set-company-phone').textContent = data.companyPhone || '+91 080 4567 8900';
      document.getElementById('set-company-address').textContent = data.companyAddress || 'Bengaluru, India';
      document.getElementById('set-currency').textContent = `${data.currencySymbol} (${data.currencyCode})`;

      document.getElementById('set-db-mode').textContent = data.databaseMode;
      document.getElementById('set-db-url').textContent = data.databaseUrl;
      document.getElementById('set-java-version').textContent = `OpenJDK ${data.javaVersion} (${data.osName})`;
      document.getElementById('set-memory').textContent = data.memoryUsage;
      document.getElementById('set-app-version').textContent = `v${data.appVersion}`;

    } catch (err) {
      console.error('Failed to load settings info:', err);
    }
  },

  setupListeners() {
    const btnChangePass = document.getElementById('btn-change-password');
    if (btnChangePass) {
      btnChangePass.addEventListener('click', () => {
        const form = document.getElementById('change-password-form');
        if (form) form.reset();
        App.openModal('modal-change-password');
      });
    }

    const btnSubmitPass = document.getElementById('btn-submit-change-password');
    if (btnSubmitPass) {
      btnSubmitPass.addEventListener('click', () => this.submitChangePassword());
    }
  },

  async submitChangePassword() {
    const curPass = document.getElementById('pass-form-current').value;
    const newPass = document.getElementById('pass-form-new').value;
    const confirmPass = document.getElementById('pass-form-confirm').value;

    if (!curPass) {
      App.showToast('Please enter your current password.', 'warning');
      return;
    }
    if (!newPass || newPass.length < 6) {
      App.showToast('New password must be at least 6 characters long.', 'warning');
      return;
    }
    if (newPass !== confirmPass) {
      App.showToast('New password and confirmation password do not match.', 'warning');
      return;
    }

    try {
      await API.changePassword(curPass, newPass);
      App.showToast('Password changed successfully!', 'success');
      App.closeModal('modal-change-password');
    } catch (err) {
      App.showToast(err.message, 'error');
    }
  }
};
