const form = document.getElementById('detailsForm');
const result = document.getElementById('result');
const skillsContainer = document.getElementById('skills');

const selectedSkillClasses = ['bg-teal-600', 'text-white', 'border-teal-600'];
const unselectedSkillClasses = ['bg-white', 'text-slate-600', 'border-slate-200'];

// Toggle a skill button's selected state
skillsContainer.addEventListener('click', (event) => {
  const btn = event.target.closest('.skill-btn');
  if (!btn) return;

  const isSelected = btn.getAttribute('aria-pressed') === 'true';
  btn.setAttribute('aria-pressed', String(!isSelected));

  if (isSelected) {
    btn.classList.remove(...selectedSkillClasses);
    btn.classList.add(...unselectedSkillClasses);
  } else {
    btn.classList.remove(...unselectedSkillClasses);
    btn.classList.add(...selectedSkillClasses);
  }
});

function getSelectedSkills() {
  return Array.from(skillsContainer.querySelectorAll('.skill-btn[aria-pressed="true"]'))
    .map((btn) => btn.dataset.skill);
}

function clearSkills() {
  skillsContainer.querySelectorAll('.skill-btn').forEach((btn) => {
    btn.setAttribute('aria-pressed', 'false');
    btn.classList.remove(...selectedSkillClasses);
    btn.classList.add(...unselectedSkillClasses);
  });
}

// Show an error message under a field
function showError(field, message) {
  const errorEl = document.querySelector(`[data-error-for="${field}"]`);
  if (errorEl) {
    errorEl.textContent = message;
    errorEl.classList.remove('hidden');
  }
}

// Clear all error messages
function clearErrors() {
  document.querySelectorAll('[data-error-for]').forEach(el => {
    el.textContent = '';
    el.classList.add('hidden');
  });
  result.classList.add('hidden');
}

function validate(data) {
  let valid = true;

  if (!data.name.trim()) {
    showError('name', 'Name is required.');
    valid = false;
  }

  if (!data.age || Number(data.age) < 1 || Number(data.age) > 120) {
    showError('age', 'Enter a valid age between 1 and 120.');
    valid = false;
  }

  if (!data.dob) {
    showError('dob', 'Date of birth is required.');
    valid = false;
  }

  if (!data.gender) {
    showError('gender', 'Please select a gender.');
    valid = false;
  }

  const phonePattern = /^[+]?[\d\s()-]{7,15}$/;
  if (!phonePattern.test(data.phone)) {
    showError('phone', 'Enter a valid phone number.');
    valid = false;
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(data.email)) {
    showError('email', 'Enter a valid email address.');
    valid = false;
  }

  if (!data.address.trim()) {
    showError('address', 'Address is required.');
    valid = false;
  }

  if (!data.skills || data.skills.length === 0) {
    showError('skills', 'Select at least one skill.');
    valid = false;
  }

  return valid;
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  clearErrors();

  const data = {
    name: form.name.value,
    age: form.age.value,
    dob: form.dob.value,
    gender: form.gender.value,
    phone: form.phone.value,
    email: form.email.value,
    address: form.address.value,
    skills: getSelectedSkills()
  };

  if (!validate(data)) {
    return;
  }

  result.innerHTML = `
        <p class="font-medium text-teal-800">Details submitted successfully!</p>
        <p class="mt-1">${data.name}, ${data.age} years, ${data.gender}</p>
        <p>${data.email} &middot; ${data.phone}</p>
        <p class="mt-1">Skills: ${data.skills.join(', ')}</p>
    `;
  result.classList.remove('hidden');
  form.reset();
});

form.addEventListener('reset', () => {
  clearErrors();
  clearSkills();
});
