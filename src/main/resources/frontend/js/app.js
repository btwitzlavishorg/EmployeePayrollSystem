/**
 * Main Application Orchestrator & UI Controller.
 * Handles SPA navigation, modal dialogs, notifications, and responsive controls.
 */
const App = {
  currentView: 'dashboard',
  user: null,
  authenticated: false,

  init() {
    this.setupLoginForm();
    this.setupDateDisplay();
    this.setupNavigation();
    this.setupMobileMenu();
    this.setupProfileDropdown();
    this.setupModals();
    this.setupGlobalSearch();

    window.addEventListener('hashchange', () => {
      if (this.authenticated) this.handleHashChange();
    });

    this.bootstrapAuth();
  },

  setupLoginForm() {
    const form = document.getElementById('login-form');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const username = document.getElementById('login-username').value.trim();
      const password = document.getElementById('login-password').value;
      const errorEl = document.getElementById('login-error');
      const btn = document.getElementById('login-submit-btn');
      if (errorEl) {
        errorEl.hidden = true;
        errorEl.textContent = '';
      }
      if (btn) {
        btn.disabled = true;
        btn.textContent = 'Signing in...';
      }
      try {
        const res = await API.login(username, password);
        if (res && res.success && res.user) {
          this.onAuthenticated(res.user);
          this.showToast('Welcome back, ' + (res.user.fullName || res.user.username), 'success');
        } else {
          throw new Error((res && res.error) || 'Login failed');
        }
      } catch (err) {
        if (errorEl) {
          errorEl.hidden = false;
          errorEl.textContent = err.message || 'Invalid username or password';
        }
      } finally {
        if (btn) {
          btn.disabled = false;
          btn.textContent = 'Sign In';
        }
      }
    });
  },

  async bootstrapAuth() {
    try {
      const res = await API.getCurrentUser();
      if (res && res.authenticated && res.user) {
        this.onAuthenticated(res.user);
        return;
      }
    } catch (ignored) {}
    this.showLoginScreen();
  },

  onAuthenticated(user) {
    this.authenticated = true;
    this.user = user;
    this.applyUserToUi(user);
    this.hideLoginScreen();
    this.handleHashChange();
  },

  applyUserToUi(user) {
    const nameEls = document.querySelectorAll('.current-user-name');
    nameEls.forEach(el => el.textContent = user.fullName || user.username);
    const roleEls = document.querySelectorAll('.current-user-role');
    roleEls.forEach(el => el.textContent = user.role || 'User');
    const avatar = document.querySelector('.user-avatar');
    if (avatar) {
      const source = (user.fullName || user.username || 'U').trim();
      const parts = source.split(/\s+/);
      avatar.textContent = (parts.length >= 2)
        ? (parts[0][0] + parts[1][0]).toUpperCase()
        : source.substring(0, 3).toUpperCase();
    }
  },

  showLoginScreen() {
    this.authenticated = false;
    this.user = null;
    const login = document.getElementById('login-screen');
    const app = document.getElementById('app-container');
    if (login) login.classList.remove('hidden');
    if (app) app.style.display = 'none';
  },

  hideLoginScreen() {
    const login = document.getElementById('login-screen');
    const app = document.getElementById('app-container');
    if (login) login.classList.add('hidden');
    if (app) app.style.display = 'flex';
  },

  setupDateDisplay() {
    const el = document.getElementById('topbar-date-display');
    if (el) {
      const now = new Date();
      el.textContent = now.toLocaleDateString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    }
  },

  setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item[data-view]');
    navItems.forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        const view = item.getAttribute('data-view');
        window.location.hash = view;
        this.closeMobileSidebar();
      });
    });

    const toggleBtn = document.getElementById('sidebar-toggle-btn');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        document.body.classList.toggle('sidebar-collapsed');
      });
    }
  },

  setupMobileMenu() {
    const mobileBtn = document.getElementById('mobile-menu-btn');
    const backdrop = document.getElementById('sidebar-backdrop');
    const sidebar = document.querySelector('.app-sidebar');

    if (mobileBtn && sidebar) {
      mobileBtn.addEventListener('click', () => {
        sidebar.classList.toggle('mobile-open');
        if (backdrop) backdrop.classList.toggle('active');
      });
    }

    if (backdrop && sidebar) {
      backdrop.addEventListener('click', () => {
        this.closeMobileSidebar();
      });
    }
  },

  closeMobileSidebar() {
    const sidebar = document.querySelector('.app-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) sidebar.classList.remove('mobile-open');
    if (backdrop) backdrop.classList.remove('active');
  },

  setupProfileDropdown() {
    const trigger = document.getElementById('profile-trigger');
    const menu = document.getElementById('profile-dropdown-menu');
    if (trigger && menu) {
      trigger.addEventListener('click', (e) => {
        e.stopPropagation();
        menu.classList.toggle('show');
      });

      document.addEventListener('click', () => {
        menu.classList.remove('show');
      });
    }

    const logoutBtn = document.getElementById('dropdown-logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        this.confirm(
          'Confirm Logout',
          'Are you sure you want to end your current session?',
          async () => {
            try {
              await API.logout();
            } catch (ignored) {}
            this.showToast('Logged out successfully', 'info');
            this.showLoginScreen();
            const form = document.getElementById('login-form');
            if (form) form.reset();
          }
        );
      });
    }
  },

  setupModals() {
    document.querySelectorAll('.modal-backdrop-custom').forEach(modal => {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          this.closeModal(modal.id);
        }
      });
    });

    document.querySelectorAll('[data-close-modal]').forEach(btn => {
      btn.addEventListener('click', () => {
        const modalId = btn.getAttribute('data-close-modal');
        this.closeModal(modalId);
      });
    });
  },

  setupGlobalSearch() {
    const searchInput = document.getElementById('topbar-search-input');
    if (searchInput) {
      searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          const val = searchInput.value.trim();
          if (val) {
            window.location.hash = 'employees';
            setTimeout(() => {
              const empSearch = document.getElementById('emp-search-input');
              if (empSearch) {
                empSearch.value = val;
                EmployeesView.loadEmployees();
              }
            }, 100);
          }
        }
      });
    }
  },

  handleHashChange() {
    let hash = window.location.hash.replace('#', '').trim();
    if (!hash) hash = 'dashboard';
    this.navigateTo(hash);
  },

  navigateTo(viewName) {
    const validViews = ['dashboard', 'employees', 'departments', 'attendance', 'salary', 'payroll', 'payslips', 'reports', 'settings'];
    if (!validViews.includes(viewName)) {
      viewName = 'dashboard';
    }

    this.currentView = viewName;

    document.querySelectorAll('.nav-item').forEach(item => {
      if (item.getAttribute('data-view') === viewName) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    document.querySelectorAll('.page-view').forEach(view => {
      if (view.id === `view-${viewName}`) {
        view.classList.add('active');
      } else {
        view.classList.remove('active');
      }
    });

    switch (viewName) {
      case 'dashboard':
        DashboardView.init();
        break;
      case 'employees':
        EmployeesView.init();
        break;
      case 'departments':
        DepartmentsView.init();
        break;
      case 'attendance':
        AttendanceView.init();
        break;
      case 'salary':
        SalaryView.init();
        break;
      case 'payroll':
        PayrollView.init();
        break;
      case 'payslips':
        PayslipsView.init();
        break;
      case 'reports':
        ReportsView.init();
        break;
      case 'settings':
        SettingsView.init();
        break;
    }
  },

  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('show');
      document.body.style.overflow = 'hidden';
    }
  },

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.remove('show');
      document.body.style.overflow = '';
    }
  },

  showToast(message, type = 'info', title = null) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast-custom toast-${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '✕';
    if (type === 'warning') icon = '⚠';

    toast.innerHTML = `
      <div class="toast-icon">${icon}</div>
      <div class="toast-content">
        <div class="toast-title">${title || (type.charAt(0).toUpperCase() + type.slice(1))}</div>
        <div class="toast-message">${message}</div>
      </div>
      <button class="toast-close">&times;</button>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => {
      toast.remove();
    });

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(20px)';
      setTimeout(() => toast.remove(), 250);
    }, 4500);
  },

  confirm(title, message, onConfirm, confirmText = 'Confirm', isDanger = false) {
    const modal = document.getElementById('modal-confirm');
    if (!modal) {
      if (window.confirm(message)) onConfirm();
      return;
    }

    document.getElementById('confirm-modal-title').textContent = title;
    document.getElementById('confirm-modal-message').textContent = message;

    const confirmBtn = document.getElementById('confirm-modal-btn');
    confirmBtn.textContent = confirmText;
    confirmBtn.className = isDanger ? 'btn-custom btn-danger-custom' : 'btn-custom btn-primary-custom';

    const newBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newBtn, confirmBtn);

    newBtn.addEventListener('click', () => {
      this.closeModal('modal-confirm');
      onConfirm();
    });

    this.openModal('modal-confirm');
  }
};

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});
