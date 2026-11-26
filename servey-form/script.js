// Script handler for freeCodeCamp Survey Form
document.addEventListener('DOMContentLoaded', () => {
    // Tab Switching Logic
    function switchTab(tabId) {
      document.querySelectorAll('.tab-content').forEach(el => el.classList.add('hidden'));
      document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('block'));
      
      document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('bg-brand-600', 'text-white');
        btn.classList.add('text-gray-400');
      });

      const activeContent = document.getElementById(`content-${tabId}`);
      if (activeContent) {
        activeContent.classList.remove('hidden');
        activeContent.classList.add('block');
      }

      const activeBtn = document.getElementById(`tab-${tabId}`);
      if (activeBtn) {
        activeBtn.classList.remove('text-gray-400');
        activeBtn.classList.add('bg-brand-600', 'text-white');
      }
    }
// 1. Hantar data ke Python Flask di latar belakang
async function handleFormSubmit(event) {
    event.preventDefault();
    
    const form = document.getElementById('survey-form');
    const formData = new FormData(form);

    try {
      const response = await fetch('http://127.0.0.1:5000/submit-survey', {
        method: 'POST',
        body: formData
      });

      const result = await response.json();
      alert(result.message);
      form.reset();
    } catch (error) {
      console.error('Error:', error);
      alert('Gagal berhubung dengan Python Flask!');
    }
  }


    // Code Copy Utility
    function copyCode(elementId) {
      const codeBlock = document.getElementById(elementId);
      if (codeBlock) {
        navigator.clipboard.writeText(codeBlock.textContent).then(() => {
          alert('Code copied to clipboard successfully!');
        });
      }
    }

    // Form Submission Handler
    function handleFormSubmit(event) {
      event.preventDefault();
      const form = document.getElementById('survey-form');
      const formData = new FormData(form);
      const name = formData.get('name') || 'Developer';

      const modal = document.getElementById('success-modal');
      const msg = document.getElementById('modal-message');
      msg.textContent = `Thank you, ${name}! Your survey response for 2026 has been successfully recorded.`;
      
      modal.classList.remove('opacity-0', 'pointer-events-none');
      modal.classList.add('opacity-100');
      form.reset();
    }

    function closeModal() {
      const modal = document.getElementById('success-modal');
      modal.classList.remove('opacity-100');
      modal.classList.add('opacity-0', 'pointer-events-none');
    }

    // Populate freeCodeCamp checklist items
    document.addEventListener('DOMContentLoaded', () => {
      const checklistGrid = document.getElementById('checklist-grid');
      if (checklistGrid) {
        const tests = [
          "1. h1 element with id=\"title\"",
          "2. title text content is not empty",
          "3. p element with id=\"description\"",
          "4. description text content is not empty",
          "5. form element with id=\"survey-form\"",
          "6. input element with id=\"name\"",
          "7. name input has type=\"text\"",
          "8. name input has required attribute",
          "9. name input has placeholder",
          "10. input element with id=\"email\"",
          "11. email input has type=\"email\"",
          "12. email input has required attribute",
          "13. email input has placeholder",
          "14. input element with id=\"number\"",
          "15. number input has type=\"number\"",
          "16. number input has min attribute",
          "17. number input has max attribute",
          "18. number input has placeholder",
          "19. label element with id=\"name-label\"",
          "20. label element with id=\"email-label\"",
          "21. label element with id=\"number-label\"",
          "22. name-label corresponds to name input",
          "23. email-label corresponds to email input",
          "24. number-label corresponds to number input",
          "25. name input is inside form",
          "26. email input is inside form",
          "27. number input is inside form",
          "28. name input has valid HTML5 markup",
          "29. email input has valid HTML5 markup",
          "30. number input has valid HTML5 markup",
          "31. select element with id=\"dropdown\"",
          "32. select has at least 2 selectable options",
          "33. select element is inside form",
          "34. radio buttons exist inside form",
          "35. radio buttons have name attribute",
          "36. radio buttons have value attribute",
          "37. multiple radio options provided",
          "38. radio inputs function correctly",
          "39. checkbox inputs exist inside form",
          "40. checkboxes have value attribute",
          "41. textarea element inside form",
          "42. button element with id=\"submit\"",
          "43. submit button has type=\"submit\"",
          "44. entire form is fully responsive"
        ];

        checklistGrid.innerHTML = tests.map(test => `
          <div class="p-2.5 rounded-xl bg-slate-900/60 border border-gray-800/80 flex items-center gap-2.5 text-gray-300">
            <i class="fa-solid fa-circle-check text-emerald-400 text-xs shrink-0"></i>
            <span class="truncate">${test}</span>
          </div>
        `).join('');
      }
    });