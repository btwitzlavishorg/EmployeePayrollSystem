/**
 * Frontend Form Validation Utility.
 * Aligns client-side form checking with Java backend ValidationUtil rules.
 */
const Validation = {
  isValidEmail(email) {
    if (!email) return false;
    const re = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return re.test(email.trim());
  },

  isValidPhone(phone) {
    if (!phone) return false;
    const cleaned = phone.replace(/[\s\-()]/g, '');
    return /^[0-9]{10}$/.test(cleaned);
  },

  isValidIfsc(ifsc) {
    if (!ifsc) return false;
    return /^[A-Za-z]{4}0[A-Za-z0-9]{6}$/.test(ifsc.trim());
  },

  isValidPan(pan) {
    if (!pan) return false;
    return /^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$/.test(pan.trim());
  },

  isPositiveNumber(val) {
    if (val === null || val === undefined || val === '') return false;
    const num = Number(val);
    return !isNaN(num) && num >= 0;
  },

  formatCurrency(amount, symbol = '₹') {
    if (amount === null || amount === undefined || isNaN(amount)) return `${symbol} 0.00`;
    const num = Number(amount);
    return symbol + ' ' + num.toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  },

  formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch {
      return dateStr;
    }
  },

  formatTime(timeStr) {
    if (!timeStr) return '--:--';
    if (timeStr.length >= 5) return timeStr.substring(0, 5);
    return timeStr;
  }
};
