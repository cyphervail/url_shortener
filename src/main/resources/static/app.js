/**
 * app.js — UI logic for snip.
 *
 * Concerns:
 *   - DOM wiring & event handling
 *   - UI state (expiry selection, history)
 *   - Calls api.js; never calls fetch directly
 *   - Maps ApiError.status → friendly copy (friendlyError)
 *
 * api.js   → network
 * app.js   → UI
 * style.css → visuals
 */

/* ─────────────────────────────────────────
   State
───────────────────────────────────────── */

/** @type {number|null} Days until expiry; null = no expiry */
let selectedExpiryDays = 7;

/** @type {Array<{ shortUrl: string, longUrl: string }>} */
let history = [];

/* ─────────────────────────────────────────
   DOM references
───────────────────────────────────────── */

const $ = id => document.getElementById(id);

const els = {
  urlInput:       $('urlInput'),
  shortenBtn:     $('shortenBtn'),
  errorMsg:       $('errorMsg'),
  expiryChips:    $('expiryChips'),
  resultCard:     $('resultCard'),
  resultUrl:      $('resultUrl'),
  expiryBadge:    $('expiryBadge'),
  copyBtn:        $('copyBtn'),
  copyLabel:      $('copyLabel'),
  historySection: $('historySection'),
  historyList:    $('historyList'),
  clearBtn:       $('clearBtn'),
};

/* ─────────────────────────────────────────
   Event wiring
───────────────────────────────────────── */

els.shortenBtn.addEventListener('click', handleShorten);

els.urlInput.addEventListener('keydown', function(e) {
  if (e.key === 'Enter') handleShorten();
});

// Event delegation for expiry pills — one listener, not N
els.expiryChips.addEventListener('click', function(e) {
  var chip = e.target.closest('.pill');
  if (!chip) return;

  document.querySelectorAll('.pill').forEach(function(p) {
    p.classList.remove('active');
  });
  chip.classList.add('active');

  var days = chip.dataset.days;
  selectedExpiryDays = days ? parseInt(days, 10) : null;
});

els.copyBtn.addEventListener('click', function() {
  copyToClipboard(els.resultUrl.textContent, els.copyBtn, els.copyLabel);
});

els.clearBtn.addEventListener('click', clearHistory);

/* ─────────────────────────────────────────
   Core handler
───────────────────────────────────────── */

async function handleShorten() {
  var url = els.urlInput.value.trim();

  clearError();

  // Client-side guard — backend will validate too, but fail fast
  if (!url) {
    showError('Paste a URL first.');
    return;
  }
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    showError('URL must start with http:// or https://');
    return;
  }

  setLoading(true);

  try {
    var expiredAt = buildExpiryTimestamp(selectedExpiryDays);
    var data = await createShortUrl(url, expiredAt); // api.js

    renderResult(data.shortUrl, expiredAt);
    addToHistory(data.shortUrl, url);
    els.urlInput.value = '';

  } catch (err) {
    showError(friendlyError(err.status));
  } finally {
    setLoading(false);
  }
}

/* ─────────────────────────────────────────
   Render helpers
───────────────────────────────────────── */

function renderResult(shortUrl, expiredAt) {
  els.resultUrl.textContent = shortUrl;
  els.resultUrl.href = shortUrl;

  els.expiryBadge.textContent = expiredAt
    ? 'Expires ' + formatDate(expiredAt)
    : 'Never expires';

  // Reset copy button
  els.copyLabel.textContent = 'Copy';
  els.copyBtn.classList.remove('copied');

  els.resultCard.classList.add('visible');
}

function addToHistory(shortUrl, longUrl) {
  history.unshift({ shortUrl: shortUrl, longUrl: longUrl });
  if (history.length > 10) history.pop();
  renderHistory();
}

function renderHistory() {
  if (history.length === 0) {
    els.historySection.classList.remove('visible');
    return;
  }

  els.historySection.classList.add('visible');

  els.historyList.innerHTML = history.map(function(entry, index) {
    return '<li class="history-item">' +
      '<span class="history-short" title="' + entry.shortUrl + '">' + entry.shortUrl + '</span>' +
      '<span class="history-long"  title="' + entry.longUrl  + '">' + entry.longUrl  + '</span>' +
      '<button class="history-copy-btn" data-index="' + index + '">Copy</button>' +
      '</li>';
  }).join('');

  // Attach listeners after innerHTML (event delegation alternative)
  els.historyList.querySelectorAll('.history-copy-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
      var entry = history[parseInt(btn.dataset.index, 10)];
      btn.textContent = 'Copied';
      navigator.clipboard.writeText(entry.shortUrl);
      setTimeout(function() { btn.textContent = 'Copy'; }, 1800);
    });
  });
}

function clearHistory() {
  history = [];
  renderHistory();
}

/* ─────────────────────────────────────────
   UI state helpers
───────────────────────────────────────── */

function setLoading(on) {
  els.shortenBtn.disabled = on;
  if (on) {
    els.shortenBtn.classList.add('loading');
  } else {
    els.shortenBtn.classList.remove('loading');
  }
}

function showError(msg) {
  els.errorMsg.textContent = msg;
}

function clearError() {
  els.errorMsg.textContent = '';
}

/**
 * Copies text, gives the button temporary feedback.
 * @param {string} text
 * @param {HTMLElement} btn
 * @param {HTMLElement} labelEl - inner span that holds the button text
 */
function copyToClipboard(text, btn, labelEl) {
  navigator.clipboard.writeText(text).then(function() {
    labelEl.textContent = 'Copied!';
    btn.classList.add('copied');
    setTimeout(function() {
      labelEl.textContent = 'Copy';
      btn.classList.remove('copied');
    }, 2000);
  });
}

/* ─────────────────────────────────────────
   Error messages
   ApiError.status → user-facing string.
   No raw server message ever reaches here.
───────────────────────────────────────── */

function friendlyError(status) {
  if (status === 0)  return "Something went wrong. Please try again.";
  if (status === 400) return "That URL does not look right.";
  if (status === 404) return "That link does not exist.";
  if (status === 429) return "Slow down a bit and try again.";
  if (status >= 500)  return "Something went wrong on our end. Try again shortly.";
  return "Something went wrong. Please try again.";
}

/* ─────────────────────────────────────────
   Date utilities
───────────────────────────────────────── */

/**
 * Builds "yyyy-MM-ddTHH:mm" — matches @JsonFormat on ShortUrlRequestDto.expiredAt.
 * @param {number|null} days
 * @returns {string|null}
 */
function buildExpiryTimestamp(days) {
  if (days === null) return null;

  var d = new Date();
  d.setDate(d.getDate() + days);

  var pad = function(n) { return String(n).padStart(2, '0'); };
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
         'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}

/**
 * "28 Mar 2026"
 * @param {string} isoString
 * @returns {string}
 */
function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}
