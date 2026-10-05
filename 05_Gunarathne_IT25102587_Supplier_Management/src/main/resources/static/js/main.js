document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.fade-in-target').forEach((el,i) => {
    el.style.animationDelay = (i*60)+'ms';
    el.classList.add('fade-in');
  });
});


// Prevent accidental double-clicks from submitting Add to Cart twice.
document.addEventListener('submit', (event) => {
  const form = event.target.closest('form[data-cart-add-form]');
  if (!form) return;

  if (form.dataset.submitting === 'true') {
    event.preventDefault();
    return;
  }

  form.dataset.submitting = 'true';
  const button = form.querySelector('button[type="submit"], button:not([type])');
  if (button) {
    button.disabled = true;
    button.dataset.originalText = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span> Adding...';
  }
});


// Cart quantity: save automatically and recalculate totals without an Update button.
document.addEventListener('DOMContentLoaded', () => {
  const quantityForms = document.querySelectorAll('.cart-qty-form');
  if (!quantityForms.length) return;

  const money = (value) => Number(value || 0).toFixed(2);

  const setText = (id, value) => {
    const el = document.getElementById(id);
    if (el) el.textContent = money(value);
  };

  const updateSummary = (data) => {
    setText('cart-original-total', data.originalTotal);
    setText('cart-discount-savings', data.discountSavings);
    setText('cart-subtotal', data.subtotal);
    setText('cart-grand-total', data.grandTotal);
    setText('free-delivery-remaining', data.freeDeliveryRemaining);

    const shipping = document.getElementById('cart-shipping');
    if (shipping) {
      shipping.textContent = data.freeDelivery ? 'FREE' : `Rs. ${money(data.shippingFee)}`;
      shipping.classList.toggle('text-success', Boolean(data.freeDelivery));
    }

    const progress = document.getElementById('free-delivery-progress');
    const earned = document.getElementById('free-delivery-earned');
    if (progress) progress.classList.toggle('d-none', Boolean(data.freeDelivery));
    if (earned) earned.classList.toggle('d-none', !data.freeDelivery);

    // Keep the amount in the header cart in sync with the cart subtotal.
    const navTotal = document.querySelector('.gc-cart strong span');
    if (navTotal) navTotal.textContent = money(data.subtotal);
  };

  quantityForms.forEach((form) => {
    const input = form.querySelector('.cart-qty-input');
    const spinner = form.querySelector('.cart-qty-saving');
    const error = form.querySelector('.cart-qty-error');
    const row = form.closest('.cart-row');
    let timer = null;
    let requestNumber = 0;

    const showError = (message) => {
      if (!error) return;
      error.textContent = message || 'Unable to update quantity.';
      error.classList.remove('d-none');
    };

    const clearError = () => {
      if (!error) return;
      error.textContent = '';
      error.classList.add('d-none');
    };

    const sendUpdate = async () => {
      const qty = Number.parseInt(input.value, 10);
      const min = Number.parseInt(input.min || '1', 10);
      const max = input.max ? Number.parseInt(input.max, 10) : null;
      const previous = Number.parseInt(input.dataset.lastValid || '1', 10);

      if (!Number.isInteger(qty) || qty < min) {
        input.value = previous;
        showError(`Quantity must be at least ${min}.`);
        return;
      }
      if (Number.isInteger(max) && qty > max) {
        input.value = previous;
        showError(`Only ${max} item(s) are currently available.`);
        return;
      }
      if (qty === previous) {
        clearError();
        return;
      }

      const thisRequest = ++requestNumber;
      clearError();
      if (spinner) spinner.classList.remove('d-none');
      input.disabled = true;

      try {
        const formData = new FormData(form);
        formData.set('qty', String(qty));

        const response = await fetch(form.action, {
          method: 'POST',
          body: formData,
          headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' },
          credentials: 'same-origin'
        });

        const data = await response.json().catch(() => ({}));
        if (thisRequest !== requestNumber) return;

        if (!response.ok || data.success !== true) {
          throw new Error(data.message || 'Unable to update quantity.');
        }

        input.value = data.quantity;
        input.dataset.lastValid = data.quantity;

        if (row) {
          const subtotal = row.querySelector('.item-subtotal');
          const savingWrap = row.querySelector('.item-saving');
          const saving = row.querySelector('.item-discount');
          if (subtotal) subtotal.textContent = money(data.itemSubtotal);
          if (saving) saving.textContent = money(data.itemDiscountAmount);
          if (savingWrap) savingWrap.classList.toggle('d-none', Number(data.itemDiscountAmount || 0) <= 0);
        }

        updateSummary(data);
      } catch (e) {
        input.value = input.dataset.lastValid || previous;
        showError(e.message);
      } finally {
        if (thisRequest === requestNumber) {
          input.disabled = false;
          if (spinner) spinner.classList.add('d-none');
          input.focus({ preventScroll: true });
        }
      }
    };

    input.addEventListener('input', () => {
      clearTimeout(timer);
      clearError();
      timer = setTimeout(sendUpdate, 350);
    });

    input.addEventListener('change', () => {
      clearTimeout(timer);
      sendUpdate();
    });
  });
});

// Global Bootstrap toasts for server-side success/error flash messages.
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.gc-flash-toast').forEach((el) => {
    if (window.bootstrap) bootstrap.Toast.getOrCreateInstance(el).show();
  });
});

// Reusable confirmation modal. Supports data-confirm on either a form or its submit button.
document.addEventListener('DOMContentLoaded', () => {
  const modalEl = document.getElementById('gcConfirmModal');
  const titleEl = document.getElementById('gcConfirmTitle');
  const messageEl = document.getElementById('gcConfirmMessage');
  const confirmBtn = document.getElementById('gcConfirmAction');
  if (!modalEl || !messageEl || !confirmBtn || !window.bootstrap) return;

  const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
  let pendingForm = null;
  let pendingButton = null;

  const openConfirmation = (form, trigger) => {
    pendingForm = form;
    pendingButton = trigger || null;
    const source = trigger?.dataset.confirm ? trigger : form;
    messageEl.textContent = source.dataset.confirm || 'Are you sure you want to continue?';
    if (titleEl) titleEl.textContent = source.dataset.confirmTitle || 'Please confirm';
    confirmBtn.textContent = source.dataset.confirmLabel || 'Confirm';
    confirmBtn.className = 'btn rounded-pill px-4 ' + (source.dataset.confirmVariant === 'warning' ? 'btn-warning' : 'btn-danger');
    modal.show();
  };

  document.addEventListener('submit', (event) => {
    const form = event.target.closest('form');
    if (!form || form.dataset.confirmed === 'true') return;
    const submitter = event.submitter;
    const trigger = submitter?.dataset.confirm ? submitter : null;
    if (!form.dataset.confirm && !trigger) return;
    event.preventDefault();
    openConfirmation(form, trigger);
  });

  confirmBtn.addEventListener('click', () => {
    if (!pendingForm) return;
    pendingForm.dataset.confirmed = 'true';
    modal.hide();
    if (pendingButton) {
      pendingForm.requestSubmit(pendingButton);
    } else {
      pendingForm.requestSubmit();
    }
    pendingForm = null;
    pendingButton = null;
  });

  modalEl.addEventListener('hidden.bs.modal', () => {
    if (pendingForm && pendingForm.dataset.confirmed !== 'true') {
      pendingForm = null;
      pendingButton = null;
    }
  });
});

// Responsive management sidebar.
document.addEventListener('DOMContentLoaded', () => {
  const shell = document.querySelector('.gc-admin-sidebar-shell');
  const openBtn = document.getElementById('gcAdminMenuToggle');
  const closeBtn = document.getElementById('gcAdminSidebarClose');
  const backdrop = document.getElementById('gcAdminSidebarBackdrop');
  if (!shell || !openBtn) return;

  const setOpen = (open) => {
    shell.classList.toggle('open', open);
    openBtn.setAttribute('aria-expanded', String(open));
    document.body.classList.toggle('gc-sidebar-open', open);
  };

  openBtn.addEventListener('click', () => setOpen(!shell.classList.contains('open')));
  closeBtn?.addEventListener('click', () => setOpen(false));
  backdrop?.addEventListener('click', () => setOpen(false));
  shell.querySelectorAll('.gc-sidebar-nav a').forEach((link) => link.addEventListener('click', () => setOpen(false)));
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && shell.classList.contains('open')) setOpen(false);
  });
  window.addEventListener('resize', () => {
    if (window.innerWidth >= 992) setOpen(false);
  });
});
