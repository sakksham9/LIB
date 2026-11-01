// PageTurn Library Frontend Application
// Detect host dynamically so it works seamlessly on localhost:8080, localhost:3000, Live Server (5500), or file://
const API = (window.location.port === '8080') ? '/api' : 'http://localhost:8080/api';
let books = [];
let currentBook = null;

// Auth Session Management
const AUTH_TOKEN_KEY = 'pageturn_auth_token';
const AUTH_USER_KEY = 'pageturn_auth_user';
let currentUser = null;

const $ = id => document.getElementById(id);

// Harmonious cover color palettes
const colors = [
  ['#dfe9d6', '#25483a'],
  ['#eadccf', '#5e3e30'],
  ['#d9e4ee', '#29455b'],
  ['#e9e3bd', '#514c24'],
  ['#e8d9e5', '#573c56'],
  ['#d4e7e4', '#28544c'],
  ['#f5e6cc', '#5c3a1e'],
  ['#e2e2ec', '#3a3a60']
];

// Helper: Escape HTML strings to prevent XSS
function esc(s = '') {
  return String(s).replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[c]));
}

// Format friendly date and time
function formatDate(isoString) {
  if (!isoString) return 'N/A';
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return isoString;
  }
}

function formatShortDate(isoString) {
  if (!isoString) return 'N/A';
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  } catch {
    return isoString;
  }
}

// Toast notification helper
function showToast(message, type = 'success') {
  const container = $('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : '⚠️'}</span> <span>${esc(message)}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    setTimeout(() => toast.remove(), 300);
  }, 3400);
}

// API request helper with Bearer token authentication
async function api(url, options = {}) {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const r = await fetch(url, {
    ...options,
    headers
  });

  if (r.status === 401 && !url.includes('/api/auth/login')) {
    if (localStorage.getItem(AUTH_TOKEN_KEY)) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USER_KEY);
      currentUser = null;
      updateAuthUI();
      showToast('Your session has expired. Please sign in again.', 'error');
    }
  }

  if (!r.ok) {
    let msg = `API error ${r.status}`;
    try {
      const errObj = await r.json();
      if (errObj.error) msg = errObj.error;
    } catch {
      msg = await r.text();
    }
    throw new Error(msg || `API error ${r.status}`);
  }
  return r.status === 204 ? null : r.json();
}

// Update UI based on authentication status and role
function updateAuthUI() {
  const badge = $('userProfileBadge');
  const signInBtn = $('signInNavBtn');
  const logoutBtn = $('logoutBtn');
  const studentDashBtn = $('studentDashboardNavBtn');
  const studentsNavBtn = $('studentsNavBtn');
  const notifBellBtn = $('notifBellBtn');
  const historyNavBtn = $('historyNavBtn');
  const addBookBtn = $('addBookBtn');
  const seedBtn = $('seedBtn');

  if (!currentUser) {
    if (badge) badge.style.display = 'none';
    if (signInBtn) signInBtn.style.display = 'inline-flex';
    if (logoutBtn) logoutBtn.style.display = 'none';
    if (studentDashBtn) studentDashBtn.style.display = 'none';
    if (studentsNavBtn) studentsNavBtn.style.display = 'none';
    if (notifBellBtn) notifBellBtn.style.display = 'none';
    if (historyNavBtn) historyNavBtn.style.display = 'none';
    if (addBookBtn) addBookBtn.style.display = 'none';
    if (seedBtn) seedBtn.style.display = 'none';
  } else if (currentUser.role === 'ADMIN') {
    if (badge) {
      badge.className = 'user-pill user-role-admin';
      badge.style.display = 'inline-flex';
      $('userAvatarChar').textContent = '🛡️';
      $('userDisplayName').textContent = currentUser.fullName || 'Admin';
      $('userRoleBadge').textContent = 'Admin';
    }
    if (signInBtn) signInBtn.style.display = 'none';
    if (logoutBtn) logoutBtn.style.display = 'inline-block';
    if (studentDashBtn) studentDashBtn.style.display = 'none';
    if (studentsNavBtn) studentsNavBtn.style.display = 'inline-flex';
    if (notifBellBtn) notifBellBtn.style.display = 'inline-flex';
    if (historyNavBtn) historyNavBtn.style.display = 'inline-flex';
    if (addBookBtn) addBookBtn.style.display = 'inline-flex';
    if (seedBtn) seedBtn.style.display = 'inline-flex';
  } else if (currentUser.role === 'STUDENT') {
    if (badge) {
      badge.className = 'user-pill user-role-student';
      badge.style.display = 'inline-flex';
      const firstChar = (currentUser.fullName || 'S').charAt(0).toUpperCase();
      $('userAvatarChar').textContent = firstChar;
      $('userDisplayName').textContent = currentUser.fullName;
      $('userRoleBadge').textContent = currentUser.studentIdNumber || 'Student';
    }
    if (signInBtn) signInBtn.style.display = 'none';
    if (logoutBtn) logoutBtn.style.display = 'inline-block';
    if (studentDashBtn) studentDashBtn.style.display = 'inline-flex';
    if (studentsNavBtn) studentsNavBtn.style.display = 'none';
    if (notifBellBtn) notifBellBtn.style.display = 'none';
    if (historyNavBtn) historyNavBtn.style.display = 'none';
    if (addBookBtn) addBookBtn.style.display = 'none';
    if (seedBtn) seedBtn.style.display = 'none';
  }

  // Refresh book card action buttons to reflect user role
  render();
}

async function initAuth() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) {
    currentUser = null;
    updateAuthUI();
    return;
  }

  try {
    const user = await api(`${API}/auth/me`);
    currentUser = user;
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  } catch (err) {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    currentUser = null;
  }
  updateAuthUI();
}

// Load books from Supabase backend
async function load() {
  try {
    const p = new URLSearchParams();
    const query = $('q').value.trim();
    const category = $('category').value;
    if (query) p.set('q', query);
    if (category) p.set('category', category);

    books = await api(`${API}/books?${p}`);
    
    // Update categories dropdown dynamically
    const currentCat = $('category').value;
    const cats = [...new Set(books.map(b => b.category).filter(Boolean))].sort();
    $('category').innerHTML = '<option value="">All categories</option>' + 
      cats.map(c => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
    if (cats.includes(currentCat)) $('category').value = currentCat;

    render();
    $('status').textContent = 'Connected to Supabase';
  } catch (e) {
    books = [];
    render();
    $('status').textContent = 'API unavailable — check Supabase connection';
    console.error(e);
  }
}

// Render book catalog cards
function render() {
  let list = [...books];
  const sort = $('sort').value;
  
  if (sort === 'rating') {
    list.sort((x, y) => (y.rating || 0) - (x.rating || 0));
  } else if (sort === 'year') {
    list.sort((x, y) => (y.publicationYear || 0) - (x.publicationYear || 0));
  } else if (sort === 'copies') {
    list.sort((x, y) => (y.availableCopies || 0) - (x.availableCopies || 0));
  } else {
    list.sort((x, y) => (x.title || '').localeCompare(y.title || ''));
  }

  $('empty').style.display = list.length > 0 ? 'none' : 'block';
  $('empty').hidden = list.length > 0;

  $('grid').innerHTML = list.map((b, i) => {
    const [bg, fg] = colors[i % colors.length];
    const copies = b.availableCopies ?? 0;
    const copiesClass = copies === 0 ? 'out-of-stock' : (copies <= 2 ? 'low-stock' : '');
    const copiesLabel = copies === 0 ? 'Out of stock' : `${copies} available`;
    const ratingStars = '★'.repeat(Math.min(5, b.rating || 0)) + '☆'.repeat(5 - Math.min(5, b.rating || 0));

    // Cover image resolver with Open Library fallback
    const coverSrc = b.coverUrl || (b.isbn ? `/images/covers/${b.isbn}.jpg` : '');
    const fallbackSrc = b.isbn ? `https://covers.openlibrary.org/b/isbn/${b.isbn}-M.jpg` : '';

    return `
      <article class="card" onclick="openBookModal(${b.id})" data-id="${b.id}" tabindex="0" role="button" aria-label="View details for ${esc(b.title)}">
        <div class="cover" style="background:${bg}; color:${fg}">
          <span class="cover-click-hint">View details ↗</span>
          ${coverSrc ? `
            <img src="${esc(coverSrc)}" alt="${esc(b.title)}" class="card-cover-img" loading="lazy"
                 onerror="if ('${fallbackSrc}' && this.src !== '${fallbackSrc}') { this.src='${fallbackSrc}'; } else { this.style.display='none'; this.nextElementSibling.style.display='flex'; }">
            <div class="cover-title-fallback" style="display:none; color:${fg};">
              <span class="cover-fallback-icon">📖</span>
              <span>${esc(b.title)}</span>
            </div>
          ` : `
            <div class="cover-title">${esc(b.title)}</div>
          `}
          <div class="book-spine-shine"></div>
        </div>
        <div class="card-body">
          <span class="tag">${esc(b.category || 'General')}</span>
          <h3>${esc(b.title)}</h3>
          <p class="author">by ${esc(b.author || 'Unknown')}</p>
          <p class="desc">${esc(b.description || 'Explore this book in the library collection.')}</p>
          
          <div class="card-footer">
            <span class="rating" title="Rating: ${b.rating || 0}/5">${ratingStars}</span>
            <span class="copies-stock ${copiesClass}">${copiesLabel}</span>
          </div>

          <div class="card-actions">
            ${copies > 0 ? `
              <button type="button" class="btn-card-action btn-borrow" onclick="event.stopPropagation(); handleBorrowClick(${b.id})">
                📥 Borrow
              </button>
            ` : `
              <button type="button" class="btn-card-action btn-card-reserve" onclick="event.stopPropagation(); handleReserveClick(${b.id})">
                🔖 Reserve
              </button>
            `}
            <button type="button" class="btn-card-action btn-details" onclick="event.stopPropagation(); openBookModal(${b.id})">
              Details
            </button>
          </div>
        </div>
      </article>
    `;
  }).join('');
}

// ================= 1. Book Details Modal =================
async function openBookModal(id) {
  currentBook = books.find(b => b.id === Number(id));
  if (!currentBook) return;

  const colorIndex = books.findIndex(b => b.id === Number(id));
  const [bg, fg] = colors[Math.max(0, colorIndex) % colors.length];

  // Set cover banner
  const modalCover = $('modalCover');
  modalCover.style.backgroundColor = bg;
  modalCover.style.color = fg;
  
  // Set cover image in modal
  const coverImg = $('modalCoverImg');
  const fallback = $('modalCoverFallback');
  const coverSrc = currentBook.coverUrl || (currentBook.isbn ? `/images/covers/${currentBook.isbn}.jpg` : '');
  const fallbackSrc = currentBook.isbn ? `https://covers.openlibrary.org/b/isbn/${currentBook.isbn}-L.jpg` : '';

  if (coverSrc) {
    coverImg.style.display = 'block';
    coverImg.src = coverSrc;
    coverImg.onerror = () => {
      if (fallbackSrc && coverImg.src !== fallbackSrc) {
        coverImg.src = fallbackSrc;
      } else {
        coverImg.style.display = 'none';
        fallback.style.display = 'flex';
      }
    };
    fallback.style.display = 'none';
  } else {
    coverImg.style.display = 'none';
    fallback.style.display = 'flex';
  }

  $('modalCategoryBadge').textContent = currentBook.category || 'General';
  $('modalCategoryBadge').style.color = fg;
  $('modalTitle').textContent = currentBook.title;
  $('modalAuthor').textContent = `by ${currentBook.author || 'Unknown'}`;

  // Meta items
  const rating = currentBook.rating || 0;
  $('modalRating').textContent = '★'.repeat(Math.min(5, rating)) + '☆'.repeat(5 - Math.min(5, rating));
  
  const copies = currentBook.availableCopies ?? 0;
  $('modalCopies').textContent = copies === 0 ? '0 available (Out of stock)' : `${copies} copy${copies > 1 ? 'ies' : ''} available`;
  $('modalCopies').style.color = copies === 0 ? 'var(--danger)' : 'var(--green)';

  $('modalYear').textContent = currentBook.publicationYear || 'N/A';
  $('modalIsbn').textContent = currentBook.isbn || 'N/A';
  $('modalDescription').textContent = currentBook.description || 'No extended description available for this volume.';

  // Borrow / Reserve button in modal
  if (copies <= 0) {
    $('borrowBtn').innerHTML = '<span>🔖</span> Reserve Book';
    $('borrowBtn').disabled = false;
    $('borrowBtn').style.opacity = '1';
    $('borrowBtn').onclick = () => handleReserveClick(currentBook.id);
  } else {
    $('borrowBtn').innerHTML = '<span>📥</span> Borrow This Book';
    $('borrowBtn').disabled = false;
    $('borrowBtn').style.opacity = '1';
    $('borrowBtn').onclick = () => handleBorrowClick(currentBook.id);
  }

  // Admin exclusive book controls
  const isAdmin = currentUser && currentUser.role === 'ADMIN';
  if ($('editBookBtn')) $('editBookBtn').style.display = isAdmin ? 'inline-flex' : 'none';
  if ($('deleteBookBtn')) $('deleteBookBtn').style.display = isAdmin ? 'inline-flex' : 'none';

  // Load borrowers for this specific book
  loadBookBorrowers(currentBook.id);

  // Show view mode, hide edit mode
  $('modalViewMode').style.display = 'block';
  $('modalViewMode').hidden = false;
  $('modalEditMode').style.display = 'none';
  $('modalEditMode').hidden = true;

  // Display modal backdrop
  $('bookModal').style.display = 'flex';
  $('bookModal').hidden = false;
  document.body.style.overflow = 'hidden';
}

function closeBookModal() {
  $('bookModal').style.display = 'none';
  $('bookModal').hidden = true;
  document.body.style.overflow = '';
  currentBook = null;
}

// Load Borrowers for a specific book inside modal
async function loadBookBorrowers(bookId) {
  const container = $('bookBorrowsList');
  container.innerHTML = '<p class="no-borrowers-hint">Loading borrower history…</p>';
  try {
    const list = await api(`${API}/books/${bookId}/borrows`);
    if (!list || list.length === 0) {
      container.innerHTML = '<p class="no-borrowers-hint">No borrow history for this book yet.</p>';
      return;
    }
    container.innerHTML = list.slice(0, 5).map(r => {
      const isBorrowed = r.status === 'BORROWED';
      const fine = r.fineAmount || 0;
      let badge = '';
      if (isBorrowed) {
        if (fine > 0) {
          badge = `<span class="badge-status badge-borrowed" style="background:#fee2e2;color:#991b1b">⚠️ Overdue (${r.daysOverdue}d late · Fine: ₹${fine})</span>`;
        } else {
          badge = `<span class="badge-status badge-borrowed">Active Loan (Due ${formatShortDate(r.dueDate)})</span>`;
        }
      } else {
        badge = fine > 0
          ? `<span class="badge-status badge-returned">Returned (Fine: ₹${fine})</span>`
          : `<span class="badge-status badge-returned">Returned (On time)</span>`;
      }

      return `
        <div class="borrower-entry">
          <div class="borrower-info">
            <span class="borrower-name-title">👤 ${esc(r.borrowerName)} ${r.borrowerContact ? `(${esc(r.borrowerContact)})` : ''}</span>
            <span class="borrower-date-info">Borrowed: ${formatDate(r.borrowedAt)} · Due: ${formatShortDate(r.dueDate)}</span>
          </div>
          <div>${badge}</div>
        </div>
      `;
    }).join('');
  } catch (err) {
    container.innerHTML = '<p class="no-borrowers-hint">Unable to load borrower history.</p>';
  }
}

// ================= 2. Borrow Book Modal (Who & When) =================
function handleBorrowClick(bookId) {
  if (!currentUser) {
    showToast('Please sign in to borrow books from the library.', 'error');
    openLoginModal('STUDENT');
    return;
  }
  openBorrowModal(bookId);
}

function handleReserveClick(bookId) {
  if (!currentUser) {
    showToast('Please sign in with your student account to place a reservation.', 'error');
    openLoginModal('STUDENT');
    return;
  }
  if (currentUser.role !== 'STUDENT') {
    showToast('Reservations are for students. As Admin, you can edit book copies.', 'error');
    return;
  }
  openReservationModal(bookId);
}

function openBorrowModal(bookId) {
  const book = books.find(b => b.id === Number(bookId));
  if (!book) return;

  if ((book.availableCopies ?? 0) <= 0) {
    showToast(`"${book.title}" is currently out of stock! You can place a reservation instead.`, 'error');
    return;
  }

  $('borrowBookId').value = book.id;
  $('borrowModalBookTitle').textContent = `"${book.title}"`;

  if (currentUser && currentUser.role === 'STUDENT') {
    $('borrowerNameInput').value = currentUser.fullName;
    $('borrowerNameInput').readOnly = true;
    $('borrowerContactInput').value = currentUser.studentIdNumber || currentUser.email || '';
    $('borrowerContactInput').readOnly = true;
  } else {
    $('borrowerNameInput').value = '';
    $('borrowerNameInput').readOnly = false;
    $('borrowerContactInput').value = '';
    $('borrowerContactInput').readOnly = false;
  }

  $('borrowDaysSelect').value = '14';

  $('borrowModal').style.display = 'flex';
  $('borrowModal').hidden = false;
  document.body.style.overflow = 'hidden';

  if (!currentUser || currentUser.role !== 'STUDENT') {
    setTimeout(() => $('borrowerNameInput').focus(), 100);
  }
}

function closeBorrowModal() {
  $('borrowModal').style.display = 'none';
  $('borrowModal').hidden = true;
  document.body.style.overflow = '';
}

// Submit Borrow Form
async function submitBorrowForm(e) {
  e.preventDefault();
  const bookId = Number($('borrowBookId').value);
  const borrowerName = $('borrowerNameInput').value.trim();
  const borrowerContact = $('borrowerContactInput').value.trim();
  const days = parseInt($('borrowDaysSelect').value, 10) || 14;

  if (!borrowerName) {
    showToast('Please enter the borrower name.', 'error');
    return;
  }

  try {
    const payload = {
      bookId,
      borrowerName,
      borrowerContact,
      days
    };

    if (currentUser && currentUser.role === 'STUDENT') {
      payload.studentId = currentUser.id;
    }

    const record = await api(`${API}/borrows`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    showToast(`✓ Book borrowed! Due on ${formatShortDate(record.dueDate)}`);
    closeBorrowModal();
    await load();

    // If book details modal is open, refresh it
    if (currentBook && currentBook.id === bookId) {
      openBookModal(bookId);
    }

    // If student dashboard is open, refresh it
    if ($('studentDashboardModal') && !$('studentDashboardModal').hidden && $('studentDashboardModal').style.display !== 'none') {
      loadStudentDashboardData();
    }
  } catch (err) {
    showToast('Borrow failed: ' + err.message, 'error');
  }
}

// ================= 3. Borrow History Modal & Late Fines =================
function openHistoryModal() {
  $('historyModal').style.display = 'flex';
  $('historyModal').hidden = false;
  document.body.style.overflow = 'hidden';
  loadBorrowRecords();
}

function closeHistoryModal() {
  $('historyModal').style.display = 'none';
  $('historyModal').hidden = true;
  document.body.style.overflow = '';
}

async function loadBorrowRecords() {
  const tbody = $('historyTableBody');
  tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">Loading borrowing records…</td></tr>';
  
  const status = $('historyStatusFilter').value;
  const p = new URLSearchParams();
  if (status) p.set('status', status);

  try {
    const list = await api(`${API}/borrows?${p}`);
    if (!list || list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">No borrow records found.</td></tr>';
      return;
    }

    tbody.innerHTML = list.map(r => {
      const isBorrowed = r.status === 'BORROWED';
      const isLost = r.status === 'LOST';

      let statusBadge = '';
      if (isBorrowed) {
        statusBadge = '<span class="badge-status badge-borrowed">Active Loan</span>';
      } else if (isLost) {
        statusBadge = '<span class="badge-status badge-lost">Reported Lost</span>';
      } else {
        statusBadge = '<span class="badge-status badge-returned">Returned</span>';
      }

      const fine = r.fineAmount || 0;
      const overdue = r.daysOverdue || 0;
      let fineHtml = '';

      if (fine > 0) {
        if (r.finePaid) {
          fineHtml = `<span class="fine-badge-paid">₹${fine} (Paid ✓)</span>`;
        } else {
          const fineLabel = isLost ? `⚠️ ₹${fine} (Lost book fee)` : `⚠️ ₹${fine} (${overdue}d late)`;
          fineHtml = `
            <div class="fine-cell">
              <span class="fine-badge-overdue" title="${fineLabel}">
                ${fineLabel}
              </span>
              <button type="button" class="btn-pay-fine" onclick="payBorrowFine(${r.id})">Pay ₹${fine}</button>
            </div>
          `;
        }
      } else {
        fineHtml = `<span class="fine-badge-zero">₹0 (On time)</span>`;
      }

      let actionBtn = '';
      if (isBorrowed) {
        actionBtn = `
          <div style="display:inline-flex; gap:4px; align-items:center;">
            <button type="button" class="btn-return-action" onclick="returnBorrowRecord(${r.id}, ${fine}, ${overdue})">Return</button>
            <button type="button" class="btn-lost-action" onclick="reportLostRecord(${r.id}, '${esc(r.bookTitle)}')">Lost?</button>
          </div>
        `;
      } else if (isLost) {
        actionBtn = `<span style="font-size:11px; color:var(--danger); font-weight:600;">Lost Copy</span>`;
      } else {
        actionBtn = `<span style="font-size:11px; color:var(--muted)">${r.returnedAt ? formatShortDate(r.returnedAt) : 'Returned'}</span>`;
      }

      const bookObj = books.find(b => b.id === r.bookId) || books.find(b => b.title === r.bookTitle);
      const thumbSrc = (bookObj && bookObj.coverUrl) || '';

      return `
        <tr>
          <td>
            <div style="display:flex; align-items:center; gap:10px;">
              ${thumbSrc ? `<img src="${esc(thumbSrc)}" alt="" class="history-book-thumb" onerror="this.style.display='none'">` : '<span class="history-thumb-placeholder">📖</span>'}
              <div>
                <strong>${esc(r.bookTitle)}</strong>
                ${bookObj && bookObj.author ? `<div style="font-size:11px;color:var(--muted)">by ${esc(bookObj.author)}</div>` : ''}
              </div>
            </div>
          </td>
          <td>👤 ${esc(r.borrowerName)}</td>
          <td>${esc(r.borrowerContact || '—')}</td>
          <td>${formatDate(r.borrowedAt)}</td>
          <td>${formatShortDate(r.dueDate)}</td>
          <td>${statusBadge}</td>
          <td>${fineHtml}</td>
          <td>${actionBtn}</td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="loading-cell" style="color:var(--danger)">Failed to load records: ${esc(err.message)}</td></tr>`;
  }
}

// Return a borrowed book by record ID (with late fine check)
async function returnBorrowRecord(recordId, fine = 0, overdue = 0) {
  if (fine > 0) {
    const ok = confirm(`⚠️ OVERDUE ALERT:\nThis book is ${overdue} day(s) past due!\nA late fee of ₹${fine} (₹10/day) has been charged.\n\nDo you want to proceed with returning the book?`);
    if (!ok) return;
  }

  try {
    const updated = await api(`${API}/borrows/${recordId}/return`, {
      method: 'POST'
    });

    if (updated.fineAmount && updated.fineAmount > 0) {
      showToast(`✓ "${updated.bookTitle}" returned! Outstanding late fine: ₹${updated.fineAmount}`, 'error');
    } else {
      showToast(`✓ "${updated.bookTitle}" returned on time! Zero fine.`);
    }

    await loadBorrowRecords();
    await load();

    if (currentBook && currentBook.id === updated.bookId) {
      openBookModal(updated.bookId);
    }
  } catch (err) {
    showToast('Failed to return book: ' + err.message, 'error');
  }
}

// Report Book as Lost (Extra fine of ₹500 + block further borrows)
async function reportLostRecord(recordId, bookTitle) {
  const ok = confirm(`⚠️ REPORT BOOK AS LOST\n\nAre you sure "${bookTitle}" has been lost?\n\n• A replacement fine penalty of ₹500 (plus any accumulated late fee) will be charged to the borrower.\n• The borrower will be BLOCKED from borrowing any new books until all fines are cleared.\n• The book copy will NOT be added back to available shelves.\n\nConfirm report lost?`);
  if (!ok) return;

  try {
    const updated = await api(`${API}/borrows/${recordId}/report-lost`, {
      method: 'POST'
    });
    showToast(`⚠️ "${updated.bookTitle}" reported as LOST! Penalty fine: ₹${updated.fineAmount}. Borrower blocked until cleared.`, 'error');
    await loadBorrowRecords();
    await load();

    if (currentBook && currentBook.id === updated.bookId) {
      openBookModal(updated.bookId);
    }
  } catch (err) {
    showToast('Failed to report lost book: ' + err.message, 'error');
  }
}

// Pay / Clear Late Fine
async function payBorrowFine(recordId) {
  try {
    const updated = await api(`${API}/borrows/${recordId}/pay-fine`, {
      method: 'POST'
    });
    showToast(`✓ Late fine of ₹${updated.fineAmount} received & marked as paid!`);
    await loadBorrowRecords();
  } catch (err) {
    showToast('Failed to pay fine: ' + err.message, 'error');
  }
}

// ================= 4. Edit Book Details =================
function openEditMode() {
  if (!currentBook) return;
  $('editTitle').value = currentBook.title || '';
  $('editAuthor').value = currentBook.author || '';
  $('editCategory').value = currentBook.category || '';
  $('editIsbn').value = currentBook.isbn || '';
  $('editYear').value = currentBook.publicationYear || '';
  $('editCopies').value = currentBook.availableCopies ?? 1;
  $('editRating').value = currentBook.rating || 5;
  $('editCoverUrl').value = currentBook.coverUrl || '';
  $('editDescription').value = currentBook.description || '';

  $('modalViewMode').style.display = 'none';
  $('modalViewMode').hidden = true;
  $('modalEditMode').style.display = 'block';
  $('modalEditMode').hidden = false;
}

function cancelEditMode() {
  $('modalViewMode').style.display = 'block';
  $('modalViewMode').hidden = false;
  $('modalEditMode').style.display = 'none';
  $('modalEditMode').hidden = true;
}

async function submitEditForm(e) {
  e.preventDefault();
  if (!currentBook) return;

  const payload = {
    title: $('editTitle').value.trim(),
    author: $('editAuthor').value.trim(),
    category: $('editCategory').value.trim(),
    isbn: $('editIsbn').value.trim(),
    publicationYear: $('editYear').value ? parseInt($('editYear').value, 10) : null,
    availableCopies: parseInt($('editCopies').value, 10) || 0,
    rating: parseInt($('editRating').value, 10) || 5,
    coverUrl: $('editCoverUrl').value.trim() || null,
    description: $('editDescription').value.trim()
  };

  try {
    await api(`${API}/books/${currentBook.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    showToast('Book details updated successfully!');
    await load();
    openBookModal(currentBook.id);
  } catch (err) {
    showToast('Failed to update book: ' + err.message, 'error');
  }
}

// Delete Book
async function deleteBook() {
  if (!currentBook) return;
  const ok = confirm(`Are you sure you want to delete "${currentBook.title}" from the library?`);
  if (!ok) return;

  try {
    await api(`${API}/books/${currentBook.id}`, { method: 'DELETE' });
    showToast(`Deleted "${currentBook.title}".`);
    closeBookModal();
    await load();
  } catch (err) {
    showToast('Failed to delete book: ' + err.message, 'error');
  }
}

// ================= 5. Add Book Modal =================
function openAddModal() {
  $('addBookForm').reset();
  $('addModal').style.display = 'flex';
  $('addModal').hidden = false;
  document.body.style.overflow = 'hidden';
}

function closeAddModal() {
  $('addModal').style.display = 'none';
  $('addModal').hidden = true;
  document.body.style.overflow = '';
}

async function submitAddBookForm(e) {
  e.preventDefault();
  const isbn = $('addIsbn').value.trim();
  const customCover = $('addCoverUrl').value.trim();
  const autoCover = customCover || (isbn ? `/images/covers/${isbn}.jpg` : null);

  const payload = {
    title: $('addTitle').value.trim(),
    author: $('addAuthor').value.trim(),
    category: $('addCategory').value.trim(),
    isbn: isbn,
    publicationYear: $('addYear').value ? parseInt($('addYear').value, 10) : null,
    availableCopies: parseInt($('addCopies').value, 10) || 1,
    rating: parseInt($('addRating').value, 10) || 5,
    coverUrl: autoCover,
    description: $('addDescription').value.trim()
  };

  try {
    const created = await api(`${API}/books`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    showToast(`Added "${created.title}" to catalog!`);
    closeAddModal();
    await load();
  } catch (err) {
    showToast('Failed to add book: ' + err.message, 'error');
  }
}

// ================= 6. Trie Autocomplete Search =================
let searchTimer;
$('q').addEventListener('input', () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(async () => {
    const prefix = $('q').value.trim();
    if (prefix.length > 1) {
      try {
        const res = await api(`${API}/dsa/suggest?prefix=${encodeURIComponent(prefix)}`);
        const suggestions = res.suggestions || [];
        $('suggestions').innerHTML = suggestions.map(s => `<button type="button">${esc(s)}</button>`).join('');
        $('suggestions').querySelectorAll('button').forEach(btn => {
          btn.onclick = () => {
            $('q').value = btn.textContent;
            $('suggestions').innerHTML = '';
            load();
          };
        });
      } catch (err) {
        $('suggestions').innerHTML = '';
      }
    } else {
      $('suggestions').innerHTML = '';
      load();
    }
  }, 220);
});

// Event Bindings
$('searchBtn').onclick = load;
$('category').onchange = load;
$('sort').onchange = render;

// Book Modal Actions
$('closeModalBtn').onclick = closeBookModal;
$('editBookBtn').onclick = openEditMode;
$('cancelEditBtn').onclick = cancelEditMode;
$('deleteBookBtn').onclick = deleteBook;
$('modalEditMode').onsubmit = submitEditForm;

// Borrow Form
$('borrowForm').onsubmit = submitBorrowForm;

// Add Book Form
$('addBookForm').onsubmit = submitAddBookForm;

// Seed Demo Books
$('seedBtn').onclick = async () => {
  try {
    const res = await api(`${API}/books/seed`, { method: 'POST' });
    showToast(res.message);
    await load();
  } catch (e) {
    showToast('Could not seed demo books', 'error');
  }
};

// ================= 7. Notifications & Reminders Center =================
async function checkNotifications() {
  try {
    const summary = await api(`${API}/notifications/summary`);
    const count = summary.totalAlertsCount || 0;
    const badge = $('notifBadge');
    if (badge) {
      badge.textContent = count;
      badge.style.display = count > 0 ? 'flex' : 'none';
    }
  } catch (err) {
    // Non-critical background check
  }
}

function openNotifModal() {
  $('notifModal').style.display = 'flex';
  $('notifModal').hidden = false;
  document.body.style.overflow = 'hidden';
  loadNotifications();
}

function closeNotifModal() {
  $('notifModal').style.display = 'none';
  $('notifModal').hidden = true;
  document.body.style.overflow = '';
}

async function loadNotifications() {
  const listEl = $('notifList');
  listEl.innerHTML = '<div class="loading-cell">Checking loans and fine notices…</div>';

  try {
    const summary = await api(`${API}/notifications/summary`);
    
    // Update metric numbers
    $('statOverdue').textContent = summary.overdueCount || 0;
    $('statDueSoon').textContent = summary.dueSoonCount || 0;
    $('statUnpaidFines').textContent = `₹${summary.totalUnpaidFines || 0}`;

    const alerts = summary.alerts || [];
    if (alerts.length === 0) {
      listEl.innerHTML = `
        <div class="empty-state" style="padding: 30px; margin: 0;">
          <div style="font-size: 32px; margin-bottom: 6px;">🎉</div>
          <h4>All Caught Up!</h4>
          <p style="font-size: 13px; color: var(--muted); margin: 0;">No books are overdue and no unpaid fines are outstanding.</p>
        </div>
      `;
      return;
    }

    listEl.innerHTML = alerts.map(a => {
      let typeBadge = '';
      if (a.type === 'LOST_PENALTY') {
        typeBadge = '<span class="fine-badge-overdue" style="background:#fee2e2; color:#991b1b;">🚫 Lost Book Penalty (₹' + a.fineAmount + ')</span>';
      } else if (a.type === 'OVERDUE') {
        typeBadge = '<span class="fine-badge-overdue">⚠️ Overdue (' + a.daysOverdue + 'd late · Fine: ₹' + a.fineAmount + ')</span>';
      } else {
        typeBadge = '<span class="fine-badge-zero" style="background:#fef3c7; color:#92400e;">⏳ Due Soon</span>';
      }

      const remindedInfo = a.reminderCount > 0
        ? `Reminder sent ${a.reminderCount} time(s) · Last: ${formatDate(a.lastRemindedAt)}`
        : `No reminder sent yet`;

      const bookObj = books.find(b => b.title === a.bookTitle);
      const thumbSrc = (bookObj && bookObj.coverUrl) || '';

      return `
        <div class="notif-card">
          ${thumbSrc ? `<img src="${esc(thumbSrc)}" alt="" class="notif-card-thumb" onerror="this.style.display='none'">` : '<span class="history-thumb-placeholder">📖</span>'}
          <div class="notif-card-info">
            <div style="display:flex; align-items:center; gap:8px;">
              <span class="notif-card-title">${esc(a.bookTitle)}</span>
              ${typeBadge}
            </div>
            <div class="notif-card-meta">
              Borrower: <strong>${esc(a.borrowerName)}</strong> (${esc(a.borrowerContact || 'No contact')}) · Due: <strong>${formatShortDate(a.dueDate)}</strong>
            </div>
            <div style="font-size: 11px; color: var(--muted); font-style: italic;">
              ${remindedInfo}
            </div>
          </div>
          <button type="button" class="primary-btn btn-send-reminder" onclick="dispatchReminder(${a.recordId})">
            📨 Send Reminder
          </button>
        </div>
      `;
    }).join('');

  } catch (err) {
    listEl.innerHTML = `<div class="loading-cell" style="color:var(--danger)">Failed to load alerts: ${esc(err.message)}</div>`;
  }
}

async function dispatchReminder(recordId) {
  try {
    const res = await api(`${API}/notifications/remind/${recordId}`, {
      method: 'POST'
    });
    showToast(`✓ Reminder notice dispatched to ${res.borrowerName} (${res.borrowerContact})!`);
    await loadNotifications();
    await checkNotifications();
  } catch (err) {
    showToast('Failed to send reminder: ' + err.message, 'error');
  }
}

// ========================================================
// 8. ROLE-BASED AUTHENTICATION (STUDENT & ADMIN)
// ========================================================

function openLoginModal(defaultType = 'STUDENT') {
  switchLoginTab(defaultType);
  $('loginUsername').value = '';
  $('loginPassword').value = '';
  $('loginModal').style.display = 'flex';
  $('loginModal').hidden = false;
  document.body.style.overflow = 'hidden';
  setTimeout(() => $('loginUsername').focus(), 100);
}

function closeLoginModal() {
  $('loginModal').style.display = 'none';
  $('loginModal').hidden = true;
  document.body.style.overflow = '';
}

function switchLoginTab(type) {
  $('loginTypeInput').value = type;
  const isStudent = (type === 'STUDENT');

  $('tabStudentBtn').className = `auth-tab-btn ${isStudent ? 'active' : ''}`;
  $('tabAdminBtn').className = `auth-tab-btn ${!isStudent ? 'active' : ''}`;

  $('studentNoticeBanner').style.display = isStudent ? 'flex' : 'none';
  $('loginUserLabel').textContent = isStudent ? 'Student ID / Username *' : 'Admin Username *';
  $('loginUsername').placeholder = isStudent ? 'e.g. STU-2026-001 or student' : 'e.g. admin';
  $('loginSubmitBtn').innerHTML = `<span>🔓</span> Sign In as ${isStudent ? 'Student' : 'Administrator'}`;
}

function fillDemoLogin(role, username, password) {
  switchLoginTab(role);
  $('loginUsername').value = username;
  $('loginPassword').value = password;
  setTimeout(() => $('loginSubmitBtn').click(), 50);
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const username = $('loginUsername').value.trim();
  const password = $('loginPassword').value;
  const loginType = $('loginTypeInput').value;

  if (!username || !password) {
    showToast('Please enter both username/ID and password.', 'error');
    return;
  }

  $('loginSubmitBtn').disabled = true;
  $('loginSubmitBtn').style.opacity = '0.7';

  try {
    const res = await api(`${API}/auth/login`, {
      method: 'POST',
      body: JSON.stringify({ username, password, loginType })
    });

    localStorage.setItem(AUTH_TOKEN_KEY, res.token);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(res));
    currentUser = res;

    updateAuthUI();
    closeLoginModal();
    showToast(`✓ Welcome back, ${res.fullName}! Signed in as ${res.role}.`);

    if (res.role === 'STUDENT') {
      openStudentDashboardModal();
    } else if (res.role === 'ADMIN') {
      checkNotifications();
    }
  } catch (err) {
    showToast('Login failed: ' + err.message, 'error');
  } finally {
    $('loginSubmitBtn').disabled = false;
    $('loginSubmitBtn').style.opacity = '1';
  }
}

async function handleLogout() {
  try {
    await api(`${API}/auth/logout`, { method: 'POST' });
  } catch (e) {
    // Continue local cleanup
  }
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  currentUser = null;

  closeStudentDashboardModal();
  closeStudentManagementModal();
  updateAuthUI();
  showToast('You have been signed out successfully.');
}

// ========================================================
// 9. STUDENT SELF-SERVICE DASHBOARD
// ========================================================

function openStudentDashboardModal() {
  if (!currentUser || currentUser.role !== 'STUDENT') {
    showToast('Please sign in as a student to access the student dashboard.', 'error');
    openLoginModal('STUDENT');
    return;
  }

  $('dashAvatar').textContent = (currentUser.fullName || 'S').charAt(0).toUpperCase();
  $('dashStudentName').textContent = currentUser.fullName;
  $('dashStudentIdNum').textContent = currentUser.studentIdNumber || 'N/A';
  $('dashDepartment').textContent = currentUser.department || 'General Studies';

  $('studentDashboardModal').style.display = 'flex';
  $('studentDashboardModal').hidden = false;
  document.body.style.overflow = 'hidden';

  switchStudentTab('loans');
  loadStudentDashboardData();
}

function closeStudentDashboardModal() {
  $('studentDashboardModal').style.display = 'none';
  $('studentDashboardModal').hidden = true;
  document.body.style.overflow = '';
}

function switchStudentTab(tab) {
  $('tabLoansBtn').className = `segmented-tab-btn ${tab === 'loans' ? 'active' : ''}`;
  $('tabReservationsBtn').className = `segmented-tab-btn ${tab === 'reservations' ? 'active' : ''}`;
  $('tabStudentHistoryBtn').className = `segmented-tab-btn ${tab === 'history' ? 'active' : ''}`;

  $('studentTabLoans').style.display = tab === 'loans' ? 'block' : 'none';
  $('studentTabReservations').style.display = tab === 'reservations' ? 'block' : 'none';
  $('studentTabHistory').style.display = tab === 'history' ? 'block' : 'none';
}

async function loadStudentDashboardData() {
  try {
    const summary = await api(`${API}/student/dashboard`);

    $('dashActiveLoansCount').textContent = summary.activeLoansCount || 0;
    $('dashDueSoonCount').textContent = (summary.overdueLoansCount || 0) + (summary.dueSoonLoansCount || 0);
    $('dashReservationsCount').textContent = summary.activeReservationsCount || 0;
    $('dashFinesAmount').textContent = `₹${summary.totalUnpaidFines || 0}`;

    if (summary.totalUnpaidFines > 0) {
      $('dashFinesAmount').style.color = 'var(--danger)';
    } else {
      $('dashFinesAmount').style.color = 'var(--green)';
    }

    // 1. Render Active Loans & Due Date Tracker
    const loans = summary.activeLoans || [];
    const loansContainer = $('studentLoansList');

    if (loans.length === 0) {
      loansContainer.innerHTML = `
        <div class="empty-state" style="padding: 24px; margin: 10px 0;">
          <div style="font-size: 28px; margin-bottom: 6px;">📖</div>
          <h4>No Active Book Loans</h4>
          <p style="font-size: 13px; color: var(--muted); margin: 0;">You do not have any borrowed books. Browse the collection below to borrow!</p>
        </div>
      `;
    } else {
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      loansContainer.innerHTML = loans.map(r => {
        const bookObj = books.find(b => b.id === r.bookId) || books.find(b => b.title === r.bookTitle);
        const thumbSrc = (bookObj && bookObj.coverUrl) || (bookObj && bookObj.isbn ? `/images/covers/${bookObj.isbn}.jpg` : '');

        // Calculate days left
        let countdownPill = '';
        const dueDate = new Date(r.dueDate);
        dueDate.setHours(0, 0, 0, 0);
        const diffDays = Math.round((dueDate - today) / (1000 * 60 * 60 * 24));

        if (diffDays < 0) {
          const lateDays = Math.abs(diffDays);
          const fine = r.fineAmount || (lateDays * 10);
          countdownPill = `<span class="due-countdown-pill due-overdue">⚠️ Overdue by ${lateDays}d · Late Fine: ₹${fine}</span>`;
        } else if (diffDays === 0) {
          countdownPill = `<span class="due-countdown-pill due-urgent">⚠️ Due Today!</span>`;
        } else if (diffDays <= 3) {
          countdownPill = `<span class="due-countdown-pill due-urgent">⏳ Due in ${diffDays} day${diffDays > 1 ? 's' : ''}</span>`;
        } else {
          countdownPill = `<span class="due-countdown-pill due-safe">✓ Due in ${diffDays} days (${formatShortDate(r.dueDate)})</span>`;
        }

        return `
          <div class="loan-item-card">
            <div class="loan-book-flex">
              ${thumbSrc ? `<img src="${esc(thumbSrc)}" alt="" class="loan-cover-thumb" onerror="this.style.display='none'">` : '<div class="loan-cover-thumb" style="display:flex;align-items:center;justify-content:center;font-size:20px;">📖</div>'}
              <div>
                <div class="loan-book-title">${esc(r.bookTitle)}</div>
                <div class="loan-book-meta">
                  Borrowed on: <strong>${formatShortDate(r.borrowedAt)}</strong> · Due Date: <strong>${formatShortDate(r.dueDate)}</strong>
                </div>
              </div>
            </div>
            <div style="display: flex; align-items: center; gap: 12px;">
              ${countdownPill}
              <button type="button" class="btn-return-action" onclick="studentReturnBook(${r.id}, ${r.fineAmount || 0}, ${r.daysOverdue || 0})">
                📥 Return Book
              </button>
            </div>
          </div>
        `;
      }).join('');
    }

    // 2. Render Reservations
    const reservations = summary.reservations || [];
    const resContainer = $('studentReservationsList');

    if (reservations.length === 0) {
      resContainer.innerHTML = `
        <div class="empty-state" style="padding: 24px; margin: 10px 0;">
          <div style="font-size: 28px; margin-bottom: 6px;">🔖</div>
          <h4>No Book Reservations</h4>
          <p style="font-size: 13px; color: var(--muted); margin: 0;">You have not reserved any books. If a book has 0 copies available, you can reserve it!</p>
        </div>
      `;
    } else {
      resContainer.innerHTML = reservations.map(res => {
        let statusBadge = '';
        if (res.status === 'READY_FOR_PICKUP') {
          statusBadge = '<span class="res-status-pill res-ready">🎉 Ready for Pickup</span>';
        } else if (res.status === 'PENDING') {
          statusBadge = '<span class="res-status-pill res-pending">⏳ Hold Pending</span>';
        } else if (res.status === 'FULFILLED') {
          statusBadge = '<span class="res-status-pill res-fulfilled">✓ Picked Up</span>';
        } else {
          statusBadge = '<span class="res-status-pill res-cancelled">Cancelled</span>';
        }

        const canCancel = (res.status === 'PENDING' || res.status === 'READY_FOR_PICKUP');

        return `
          <div class="res-item-card">
            <div>
              <div style="display: flex; align-items: center; gap: 8px;">
                <strong style="font-size: 15px; color: var(--ink);">${esc(res.bookTitle)}</strong>
                ${statusBadge}
              </div>
              <div style="font-size: 12px; color: var(--muted); margin-top: 4px;">
                Reserved on: <strong>${formatDate(res.reservedAt)}</strong> · Valid until: <strong>${formatShortDate(res.expiryDate)}</strong>
                ${res.notes ? ` · Note: <em>"${esc(res.notes)}"</em>` : ''}
              </div>
            </div>
            ${canCancel ? `
              <button type="button" class="btn-lost-action" onclick="cancelStudentReservation(${res.id})">
                Cancel Hold
              </button>
            ` : ''}
          </div>
        `;
      }).join('');
    }

    // 3. Render Student Borrowing History
    const historyList = await api(`${API}/student/history`);
    const historyTbody = $('studentHistoryTableBody');

    if (!historyList || historyList.length === 0) {
      historyTbody.innerHTML = '<tr><td colspan="5" class="loading-cell">No borrowing history yet.</td></tr>';
    } else {
      historyTbody.innerHTML = historyList.map(r => {
        let badge = '';
        if (r.status === 'BORROWED') {
          badge = '<span class="badge-status badge-borrowed">Active Loan</span>';
        } else if (r.status === 'LOST') {
          badge = '<span class="badge-status badge-lost">Reported Lost</span>';
        } else {
          badge = '<span class="badge-status badge-returned">Returned</span>';
        }

        let fineDetails = '₹0';
        if (r.fineAmount > 0) {
          fineDetails = r.finePaid
            ? `<span class="fine-badge-paid">₹${r.fineAmount} (Paid ✓)</span>`
            : `<span class="fine-badge-overdue">₹${r.fineAmount} (Unpaid)</span>`;
        }

        return `
          <tr>
            <td><strong>${esc(r.bookTitle)}</strong></td>
            <td>${formatDate(r.borrowedAt)}</td>
            <td>${formatShortDate(r.dueDate)}</td>
            <td>${badge}</td>
            <td>${fineDetails}</td>
          </tr>
        `;
      }).join('');
    }

  } catch (err) {
    showToast('Failed to load student dashboard: ' + err.message, 'error');
  }
}

async function studentReturnBook(recordId, fine = 0, overdue = 0) {
  if (fine > 0) {
    const ok = confirm(`⚠️ Overdue Return:\nThis book is ${overdue} day(s) overdue!\nA late fee of ₹${fine} will be recorded on your account.\n\nProceed to return?`);
    if (!ok) return;
  }

  try {
    const updated = await api(`${API}/student/return/${recordId}`, { method: 'POST' });
    showToast(`✓ "${updated.bookTitle}" returned successfully!`);
    await loadStudentDashboardData();
    await load();
  } catch (err) {
    showToast('Return failed: ' + err.message, 'error');
  }
}

// ========================================================
// 10. BOOK RESERVATIONS
// ========================================================

function openReservationModal(bookId) {
  const book = books.find(b => b.id === Number(bookId));
  if (!book) return;

  $('reserveBookId').value = book.id;
  $('reserveModalBookTitle').textContent = `"${book.title}"`;
  $('reserveNotes').value = '';

  $('reservationModal').style.display = 'flex';
  $('reservationModal').hidden = false;
  document.body.style.overflow = 'hidden';
}

function closeReservationModal() {
  $('reservationModal').style.display = 'none';
  $('reservationModal').hidden = true;
  document.body.style.overflow = '';
}

async function handleReservationSubmit(e) {
  e.preventDefault();
  const bookId = Number($('reserveBookId').value);
  const notes = $('reserveNotes').value.trim();

  try {
    const res = await api(`${API}/student/reservations`, {
      method: 'POST',
      body: JSON.stringify({ bookId, notes })
    });

    showToast(`✓ Reservation confirmed for "${res.bookTitle}"! Held for 7 days upon arrival.`);
    closeReservationModal();

    if ($('studentDashboardModal') && !$('studentDashboardModal').hidden && $('studentDashboardModal').style.display !== 'none') {
      loadStudentDashboardData();
    }
  } catch (err) {
    showToast('Reservation failed: ' + err.message, 'error');
  }
}

async function cancelStudentReservation(reservationId) {
  const ok = confirm('Are you sure you want to cancel this book reservation hold?');
  if (!ok) return;

  try {
    await api(`${API}/student/reservations/${reservationId}/cancel`, { method: 'POST' });
    showToast('Reservation hold cancelled.');
    await loadStudentDashboardData();
  } catch (err) {
    showToast('Cancellation failed: ' + err.message, 'error');
  }
}

// ========================================================
// 11. ADMIN STUDENT DIRECTORY & REGISTRATION CONTROL
// ========================================================

let allStudentsCache = [];

function openStudentManagementModal() {
  if (!currentUser || currentUser.role !== 'ADMIN') {
    showToast('Access Denied: Only Library Administrators can manage student records.', 'error');
    return;
  }

  $('studentManagementModal').style.display = 'flex';
  $('studentManagementModal').hidden = false;
  document.body.style.overflow = 'hidden';

  loadStudentsList();
}

function closeStudentManagementModal() {
  $('studentManagementModal').style.display = 'none';
  $('studentManagementModal').hidden = true;
  document.body.style.overflow = '';
}

async function loadStudentsList() {
  const tbody = $('studentsTableBody');
  tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">Loading student records…</td></tr>';

  try {
    allStudentsCache = await api(`${API}/admin/students`);
    filterStudentsTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="loading-cell" style="color:var(--danger)">Failed to load students: ${esc(err.message)}</td></tr>`;
  }
}

function filterStudentsTable() {
  const query = ($('studentSearchInput').value || '').trim().toLowerCase();
  const statusFilter = $('studentStatusFilter').value;
  const tbody = $('studentsTableBody');

  let list = allStudentsCache.filter(s => {
    if (statusFilter && s.status !== statusFilter) return false;
    if (query) {
      const matchName = (s.fullName || '').toLowerCase().includes(query);
      const matchId = (s.studentIdNumber || '').toLowerCase().includes(query);
      const matchDept = (s.department || '').toLowerCase().includes(query);
      const matchEmail = (s.email || '').toLowerCase().includes(query);
      return matchName || matchId || matchDept || matchEmail;
    }
    return true;
  });

  if (list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">No student records match filter.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(s => {
    const isActive = (s.status === 'ACTIVE');
    const statusPill = isActive
      ? '<span class="status-badge-active">● Active</span>'
      : '<span class="status-badge-deactivated">● Deactivated</span>';

    const fineHtml = (s.totalUnpaidFines > 0)
      ? `<span class="fine-badge-overdue" style="font-size:11px;">⚠️ ₹${s.totalUnpaidFines}</span>`
      : `<span class="fine-badge-zero" style="font-size:11px;">₹0</span>`;

    return `
      <tr>
        <td><strong style="color:var(--ink);">${esc(s.studentIdNumber || '—')}</strong></td>
        <td>
          <div style="font-weight:700;">${esc(s.fullName)}</div>
          <div style="font-size:11px;color:var(--muted)">Username: ${esc(s.username)}</div>
        </td>
        <td>
          <div>${esc(s.email || 'No email')}</div>
          <div style="font-size:11px;color:var(--muted)">${esc(s.phone || 'No phone')}</div>
        </td>
        <td>${esc(s.department || 'General')}</td>
        <td>${statusPill}</td>
        <td><strong>${s.activeLoansCount || 0}</strong> active</td>
        <td>${fineHtml}</td>
        <td>
          <div style="display: inline-flex; gap: 4px; align-items: center; flex-wrap: wrap;">
            <button type="button" class="btn-card-action" style="padding:4px 8px;font-size:11px;" onclick="openEditStudentModal(${s.id})">
              ✎ Edit
            </button>
            <button type="button" class="${isActive ? 'btn-lost-action' : 'btn-return-action'}" style="padding:4px 8px;font-size:11px;" onclick="toggleStudentStatus(${s.id}, '${esc(s.fullName)}', '${s.status}')">
              ${isActive ? 'Deactivate' : 'Activate'}
            </button>
            <button type="button" class="btn-card-action" style="padding:4px 8px;font-size:11px;" onclick="promptResetPassword(${s.id}, '${esc(s.fullName)}')">
              🔑 Reset
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

// Register Student Form
function openRegisterStudentModal() {
  $('registerStudentForm').reset();
  $('registerStudentModal').style.display = 'flex';
  $('registerStudentModal').hidden = false;
  document.body.style.overflow = 'hidden';
  setTimeout(() => $('regStudentId').focus(), 100);
}

function closeRegisterStudentModal() {
  $('registerStudentModal').style.display = 'none';
  $('registerStudentModal').hidden = true;
  document.body.style.overflow = '';
}

async function handleRegisterStudentSubmit(e) {
  e.preventDefault();
  const payload = {
    studentIdNumber: $('regStudentId').value.trim(),
    fullName: $('regFullName').value.trim(),
    email: $('regEmail').value.trim() || null,
    phone: $('regPhone').value.trim() || null,
    department: $('regDepartment').value.trim() || null,
    password: $('regPassword').value.trim() || null
  };

  try {
    const created = await api(`${API}/admin/students`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    showToast(`✓ Student "${created.fullName}" (ID: ${created.studentIdNumber}) registered successfully!`);
    closeRegisterStudentModal();
    loadStudentsList();
  } catch (err) {
    showToast('Failed to register student: ' + err.message, 'error');
  }
}

// Edit Student Form
function openEditStudentModal(id) {
  const student = allStudentsCache.find(s => s.id === id);
  if (!student) return;

  $('editStudentId').value = student.id;
  $('editStudentIdDisplay').textContent = `${student.fullName} (${student.studentIdNumber})`;
  $('editStudentFullName').value = student.fullName || '';
  $('editStudentEmail').value = student.email || '';
  $('editStudentPhone').value = student.phone || '';
  $('editStudentDepartment').value = student.department || '';
  $('editStudentNewPassword').value = '';

  $('editStudentModal').style.display = 'flex';
  $('editStudentModal').hidden = false;
  document.body.style.overflow = 'hidden';
}

function closeEditStudentModal() {
  $('editStudentModal').style.display = 'none';
  $('editStudentModal').hidden = true;
  document.body.style.overflow = '';
}

async function handleEditStudentSubmit(e) {
  e.preventDefault();
  const id = Number($('editStudentId').value);
  const payload = {
    fullName: $('editStudentFullName').value.trim(),
    email: $('editStudentEmail').value.trim() || null,
    phone: $('editStudentPhone').value.trim() || null,
    department: $('editStudentDepartment').value.trim() || null,
    password: $('editStudentNewPassword').value.trim() || null
  };

  try {
    await api(`${API}/admin/students/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });

    showToast('Student details updated successfully!');
    closeEditStudentModal();
    loadStudentsList();
  } catch (err) {
    showToast('Update failed: ' + err.message, 'error');
  }
}

async function toggleStudentStatus(id, name, currentStatus) {
  const isCurrentlyActive = (currentStatus === 'ACTIVE');
  const actionText = isCurrentlyActive ? 'DEACTIVATE' : 'ACTIVATE';
  const ok = confirm(`Are you sure you want to ${actionText} the student account for "${name}"?\n\n${isCurrentlyActive ? '• The student will be logged out and cannot sign in until reactivated.' : '• The student will regain login and borrowing access.'}`);
  if (!ok) return;

  try {
    const updated = await api(`${API}/admin/students/${id}/status`, { method: 'PATCH' });
    showToast(`✓ Account for "${name}" is now ${updated.status}.`);
    loadStudentsList();
  } catch (err) {
    showToast('Failed to update status: ' + err.message, 'error');
  }
}

async function promptResetPassword(id, name) {
  const newPass = prompt(`Enter new password for student "${name}":`, 'student123');
  if (!newPass || !newPass.trim()) return;

  try {
    await api(`${API}/admin/students/${id}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ newPassword: newPass.trim() })
    });
    showToast(`✓ Password successfully reset for "${name}".`);
  } catch (err) {
    showToast('Failed to reset password: ' + err.message, 'error');
  }
}

// ========================================================
// 12. BACKDROP CLICK AND GLOBAL EVENT BINDINGS
// ========================================================

$('bookModal').addEventListener('click', (e) => {
  if (e.target === $('bookModal')) closeBookModal();
});
$('borrowModal').addEventListener('click', (e) => {
  if (e.target === $('borrowModal')) closeBorrowModal();
});
$('historyModal').addEventListener('click', (e) => {
  if (e.target === $('historyModal')) closeHistoryModal();
});
$('notifModal').addEventListener('click', (e) => {
  if (e.target === $('notifModal')) closeNotifModal();
});
$('addModal').addEventListener('click', (e) => {
  if (e.target === $('addModal')) closeAddModal();
});
$('loginModal').addEventListener('click', (e) => {
  if (e.target === $('loginModal')) closeLoginModal();
});
$('studentDashboardModal').addEventListener('click', (e) => {
  if (e.target === $('studentDashboardModal')) closeStudentDashboardModal();
});
$('studentManagementModal').addEventListener('click', (e) => {
  if (e.target === $('studentManagementModal')) closeStudentManagementModal();
});
$('registerStudentModal').addEventListener('click', (e) => {
  if (e.target === $('registerStudentModal')) closeRegisterStudentModal();
});
$('editStudentModal').addEventListener('click', (e) => {
  if (e.target === $('editStudentModal')) closeEditStudentModal();
});
$('reservationModal').addEventListener('click', (e) => {
  if (e.target === $('reservationModal')) closeReservationModal();
});

// Escape key listener for all modals
window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeBookModal();
    closeBorrowModal();
    closeHistoryModal();
    closeNotifModal();
    closeAddModal();
    closeLoginModal();
    closeStudentDashboardModal();
    closeStudentManagementModal();
    closeRegisterStudentModal();
    closeEditStudentModal();
    closeReservationModal();
  }
});

// Window Exports
window.openBookModal = openBookModal;
window.closeBookModal = closeBookModal;
window.openBorrowModal = openBorrowModal;
window.closeBorrowModal = closeBorrowModal;
window.handleBorrowClick = handleBorrowClick;
window.handleReserveClick = handleReserveClick;
window.openHistoryModal = openHistoryModal;
window.closeHistoryModal = closeHistoryModal;
window.loadBorrowRecords = loadBorrowRecords;
window.returnBorrowRecord = returnBorrowRecord;
window.reportLostRecord = reportLostRecord;
window.payBorrowFine = payBorrowFine;
window.openNotifModal = openNotifModal;
window.closeNotifModal = closeNotifModal;
window.loadNotifications = loadNotifications;
window.dispatchReminder = dispatchReminder;
window.openAddModal = openAddModal;
window.closeAddModal = closeAddModal;

window.openLoginModal = openLoginModal;
window.closeLoginModal = closeLoginModal;
window.switchLoginTab = switchLoginTab;
window.fillDemoLogin = fillDemoLogin;
window.handleLoginSubmit = handleLoginSubmit;
window.handleLogout = handleLogout;

window.openStudentDashboardModal = openStudentDashboardModal;
window.closeStudentDashboardModal = closeStudentDashboardModal;
window.switchStudentTab = switchStudentTab;
window.loadStudentDashboardData = loadStudentDashboardData;
window.studentReturnBook = studentReturnBook;

window.openReservationModal = openReservationModal;
window.closeReservationModal = closeReservationModal;
window.handleReservationSubmit = handleReservationSubmit;
window.cancelStudentReservation = cancelStudentReservation;

window.openStudentManagementModal = openStudentManagementModal;
window.closeStudentManagementModal = closeStudentManagementModal;
window.loadStudentsList = loadStudentsList;
window.filterStudentsTable = filterStudentsTable;
window.openRegisterStudentModal = openRegisterStudentModal;
window.closeRegisterStudentModal = closeRegisterStudentModal;
window.handleRegisterStudentSubmit = handleRegisterStudentSubmit;
window.openEditStudentModal = openEditStudentModal;
window.closeEditStudentModal = closeEditStudentModal;
window.handleEditStudentSubmit = handleEditStudentSubmit;
window.toggleStudentStatus = toggleStudentStatus;
window.promptResetPassword = promptResetPassword;

// Initial Load & Auth Verification
initAuth();
load();
checkNotifications();

