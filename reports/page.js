'use strict';
// The form page and its script. Served by the function itself: no CDN, no third party.

const escapeHtml = (s) => String(s ?? '')
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

const { FOOTER, FOLLOW_UP } = require('./report');

const STYLE = `
  :root { color-scheme: light dark; --rose: #c2415d; --bg: #fff8f7; --fg: #2a1f21; --muted: #6e5a5e; --line: #ebd6da; --card: #fff; }
  @media (prefers-color-scheme: dark) { :root { --bg: #1b1416; --fg: #f0dee1; --muted: #c3aeb2; --line: #3e3134; --rose: #ff8fa6; --card: #261d1f; } }
  body { margin: 0; background: var(--bg); color: var(--fg); font: 16px/1.5 system-ui, sans-serif; }
  main { max-width: 640px; margin: 0 auto; padding: 24px 16px 48px; }
  h1 { font-size: 1.5rem; margin: 0 0 4px; }
  h2 { font-size: 1.1rem; margin: 24px 0 6px; }
  p.lead { color: var(--muted); margin: 0 0 20px; }
  label { display: block; font-weight: 600; margin: 16px 0 6px; }
  input, textarea, select { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 1px solid var(--line);
    border-radius: 12px; background: transparent; color: inherit; font: inherit; }
  textarea { min-height: 180px; }
  .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  @media (max-width: 480px) { .row { grid-template-columns: 1fr; } }
  button { margin-top: 20px; width: 100%; padding: 14px; border: 0; border-radius: 14px; background: var(--rose);
    color: #fff; font: inherit; font-weight: 700; cursor: pointer; }
  button:disabled { opacity: .6; cursor: wait; }
  .note { color: var(--muted); font-size: .9rem; }
  pre { white-space: pre-wrap; word-break: break-word; background: var(--card); border: 1px solid var(--line);
    border-radius: 12px; padding: 12px; font-size: .85rem; }
  #status { margin-top: 16px; font-weight: 600; }
  a { color: var(--rose); }
`;

function formPage(query = {}) {
  const kind = query.kind === 'feature' ? 'feature' : 'bug';
  const followUp = FOLLOW_UP.test(String(query.followUp || '')) ? String(query.followUp) : '';
  const crash = query.crash === '1' ? 'true' : '';
  const v = (name, max) => escapeHtml(String(query[name] ?? '').slice(0, max));
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="referrer" content="no-referrer">
<title>Ondea · anonymous report</title>
<style>${STYLE}</style>
</head>
<body>
<main>
  <h1>Ondea · anonymous report</h1>
  <p class="lead">Anonymous: no account, no email. Nothing about you is kept: not your IP address, not a cookie.
  Never write health data here.</p>
  <noscript><p class="note" style="font-weight:600">This form needs JavaScript. Turn it on for this page, or send the report with GitHub.</p></noscript>
  <form id="report">
    <label for="kind">Type</label>
    <select id="kind" name="kind">
      <option value="bug"${kind === 'bug' ? ' selected' : ''}>Problem</option>
      <option value="feature"${kind === 'feature' ? ' selected' : ''}>Idea for a feature</option>
    </select>
    <label for="title">Summary</label>
    <input id="title" name="title" maxlength="120" required minlength="5" value="${v('title', 120)}">
    <label for="details">Details</label>
    <textarea id="details" name="details" maxlength="5000" required minlength="10">${v('what', 5000)}</textarea>
    <div class="row">
      <div><label for="version">App version</label><input id="version" name="version" maxlength="40" value="${v('version', 40)}"></div>
      <div><label for="device">Phone</label><input id="device" name="device" maxlength="120" value="${v('device', 120)}"></div>
    </div>
    <input type="hidden" name="followUp" value="${followUp}">
    <input type="hidden" name="crash" value="${crash}">
    <h2>Preview of the issue</h2>
    <p class="note">This exact text goes to the maintainer's private list. Nothing else is sent.</p>
    <pre id="preview" data-footer="${escapeHtml(FOOTER)}"></pre>
    <button id="send" type="submit">Send</button>
    <p id="status" role="status" aria-live="polite"></p>
    <pre id="log" hidden></pre>
    <p class="note">Before sending, your browser solves a small puzzle for a second or two. It stops spam without tracking you.</p>
  </form>
</main>
<script src="form.js"></script>
</body>
</html>`;
}

const formScript = `'use strict';
// Debug mode (?debug=1 in the address): each step is shown on the page. Nothing is sent or stored.
const logBox = document.getElementById('log');
const debug = new URLSearchParams(location.search).has('debug');
const t0 = Date.now();
function log(text) {
  if (!debug) return;
  logBox.hidden = false;
  logBox.textContent += ((Date.now() - t0) / 1000).toFixed(1) + ' s  ' + text + '\\n';
}
window.addEventListener('error', (e) => {
  log('Error: ' + e.message + ' (line ' + e.lineno + ')');
  document.getElementById('status').textContent = 'Page error: ' + e.message;
});
window.addEventListener('unhandledrejection', (e) => log('Error: ' + (e.reason && e.reason.message || e.reason)));
log('Script loaded. ' + navigator.userAgent);
log('Address: ' + location.origin + location.pathname);
const form = document.getElementById('report');
const status = document.getElementById('status');
const button = document.getElementById('send');
const preview = document.getElementById('preview');

// Same format as the issue (report.js toIssue).
function showPreview() {
  const d = Object.fromEntries(new FormData(form));
  const prefix = d.kind === 'bug' ? 'Bug' : 'Idea';
  preview.textContent = prefix + ': ' + d.title.trim() + '\\n\\n' + d.details.trim() + '\\n\\n---\\n' +
    'App version: ' + (d.version.trim() || 'not given') + '\\n' + 'Device: ' + (d.device.trim() || 'not given') +
    '\\n\\n' + preview.dataset.footer;
}
form.addEventListener('input', showPreview);
showPreview();

// Plain SHA-256: much faster than one crypto.subtle call per try, and the same in every browser.
const K = new Uint32Array([
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2]);
const W = new Uint32Array(64);
function sha256(text) {
  const bytes = new TextEncoder().encode(text);
  const blocks = Math.ceil((bytes.length + 9) / 64);
  const data = new Uint8Array(blocks * 64);
  data.set(bytes);
  data[bytes.length] = 0x80;
  const bits = bytes.length * 8;
  data[data.length - 4] = bits >>> 24; data[data.length - 3] = bits >>> 16;
  data[data.length - 2] = bits >>> 8; data[data.length - 1] = bits;
  const h = new Uint32Array([0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]);
  const rotr = (x, n) => (x >>> n) | (x << (32 - n));
  for (let off = 0; off < data.length; off += 64) {
    for (let i = 0; i < 16; i++) {
      W[i] = (data[off + 4 * i] << 24) | (data[off + 4 * i + 1] << 16) | (data[off + 4 * i + 2] << 8) | data[off + 4 * i + 3];
    }
    for (let i = 16; i < 64; i++) {
      const s0 = rotr(W[i - 15], 7) ^ rotr(W[i - 15], 18) ^ (W[i - 15] >>> 3);
      const s1 = rotr(W[i - 2], 17) ^ rotr(W[i - 2], 19) ^ (W[i - 2] >>> 10);
      W[i] = W[i - 16] + s0 + W[i - 7] + s1;
    }
    let [a, b, c, d, e, f, g, k] = h;
    for (let i = 0; i < 64; i++) {
      const t1 = (k + (rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)) + ((e & f) ^ (~e & g)) + K[i] + W[i]) >>> 0;
      const t2 = ((rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)) + ((a & b) ^ (a & c) ^ (b & c))) >>> 0;
      k = g; g = f; f = e; e = (d + t1) >>> 0; d = c; c = b; b = a; a = (t1 + t2) >>> 0;
    }
    h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += k;
  }
  return Array.from(h, (x) => x.toString(16).padStart(8, '0')).join('');
}

// Tries the numbers in slices, so the page stays responsive and shows progress.
async function solve(c, onProgress) {
  const slice = 2000;
  for (let start = 0; start <= c.maxnumber; start += slice) {
    const end = Math.min(start + slice, c.maxnumber + 1);
    for (let n = start; n < end; n++) {
      if (sha256(c.salt + n) === c.challenge) {
        return { algorithm: c.algorithm, challenge: c.challenge, number: n, salt: c.salt, signature: c.signature };
      }
    }
    onProgress(Math.round((100 * end) / (c.maxnumber + 1)));
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
  throw new Error('puzzle');
}

function show(text) {
  status.textContent = text;
  if (text) status.scrollIntoView({ block: 'center', behavior: 'smooth' });
}

form.addEventListener('submit', async (event) => {
  log('Send pressed.');
  event.preventDefault();
  button.disabled = true;
  button.textContent = 'Checking…';
  show('');
  try {
    const answer = await fetch('challenge', { cache: 'no-store' });
    if (!answer.ok) throw new Error('challenge ' + answer.status);
    const challenge = await answer.json();
    log('Puzzle received (HTTP ' + answer.status + ').');
    const started = Date.now();
    const proof = await solve(challenge, (percent) => { button.textContent = 'Checking… ' + percent + ' %'; });
    button.textContent = 'Sending…';
    const data = Object.fromEntries(new FormData(form));
    log('Puzzle solved in ' + ((Date.now() - started) / 1000).toFixed(1) + ' s.');
    const response = await fetch('report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...data, proof }),
    });
    log('Report answer: HTTP ' + response.status + '.');
    if (response.ok) {
      const code = form.elements.followUp.value;
      form.reset();
      showPreview();
      show(code
        ? 'Thank you. Your report was sent. Ondea keeps its follow-up link in Settings → My reports.'
        : 'Thank you. Your report was sent.');
    } else {
      const { error } = await response.json().catch(() => ({}));
      show(error === 'title' ? 'Please write a summary (5 characters or more).'
        : error === 'details' ? 'Please add details (10 characters or more).'
        : 'The report could not be sent. Please try again later. (' + (error || response.status) + ')');
    }
  } catch (e) {
    log('Error: ' + (e && e.message || e));
    show('The report could not be sent. Check your connection and try again. (' + (e && e.message || 'error') + ')');
  } finally {
    button.disabled = false;
    button.textContent = 'Send';
  }
});
`;

const STATUS_TEXT = {
  received: ['Received', 'The maintainer has your report.'],
  published: ['Published', 'The maintainer published your report. Follow it here:'],
  closed: ['Closed', 'The maintainer closed this report.'],
  declined: ['Closed: not planned', 'The maintainer read this report and does not plan to change the app for it.'],
  unknown: ['Not found', 'No report has this follow-up code. Maybe it was not sent: open the form from Ondea again.'],
  invalid: ['Not found', 'This follow-up link is not valid.'],
  error: ['Not available', 'The status cannot be read now. Please try again later.'],
};

/** Status of one report, for "My reports" in Ondea (opened in the browser). */
function statusPage({ status, public: link }) {
  const [title, text] = STATUS_TEXT[status] || STATUS_TEXT.unknown;
  const safeLink = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/issues\/\d+$/.test(link || '') ? link : '';
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="referrer" content="no-referrer">
<title>Ondea · report status</title>
<style>${STYLE}</style>
</head>
<body>
<main>
  <p class="note">Ondea · anonymous report</p>
  <h1>${escapeHtml(title)}</h1>
  <p>${escapeHtml(text)}</p>
  ${safeLink ? `<p><a href="${escapeHtml(safeLink)}" rel="noreferrer">${escapeHtml(safeLink)}</a></p>` : ''}
  <p class="note">Only the status is shown here, not the maintainer's comments.</p>
</main>
</body>
</html>`;
}

module.exports = { formPage, formScript, statusPage, escapeHtml };
