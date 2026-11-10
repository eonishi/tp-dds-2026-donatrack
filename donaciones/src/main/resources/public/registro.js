(() => {
  const form = document.querySelector('.registro-form');
  if (!form) return;

  const syncPanels = () => {
    const role = form.elements.rol.value;
    const person = form.elements.donanteTipoPersona.value;
    form.querySelectorAll('.panel-rol').forEach(panel => {
      const active = panel.dataset.rol === role;
      panel.hidden = !active;
      panel.querySelectorAll('input, select').forEach(input => { input.disabled = !active; });
      panel.querySelectorAll('[data-persona]').forEach(group => {
        const selected = active && group.dataset.persona === person;
        group.hidden = !selected;
        group.disabled = !selected;
        group.querySelectorAll('input, select').forEach(input => { input.disabled = !selected; });
      });
    });
  };

  const syncContacts = () => {
    const phone = form.elements.donanteTelefono;
    const preferred = form.elements.donanteMedioContacto;
    const hasPhone = /^\+[1-9][0-9]{1,14}$/.test(phone.value.trim());
    [...preferred.options].forEach(option => {
      option.disabled = option.value !== 'CORREO' && !hasPhone;
    });
    if (!hasPhone) preferred.value = 'CORREO';
  };

  form.querySelectorAll('[name="rol"], [name="donanteTipoPersona"]').forEach(input => {
    input.addEventListener('change', syncPanels);
  });
  form.elements.donanteTelefono.addEventListener('input', syncContacts);
  syncPanels();
  syncContacts();
})();
