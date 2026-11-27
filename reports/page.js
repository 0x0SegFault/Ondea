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
    <p class="note">Before sending, your browser solves a small puzzle for a second or two. It stops spam without tracking you.</p>
    <p id="status" role="status" aria-live="polite"></p>
  </form>
</main>
<script src="form.js"></script>
</body>
</html>`;
}

const formScript = `'use strict';
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

async function sha256(text) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
}

async function solve(c) {
  for (let n = 0; n <= c.maxnumber; n++) {
    if (await sha256(c.salt + n) === c.challenge) {
      return { algorithm: c.algorithm, challenge: c.challenge, number: n, salt: c.salt, signature: c.signature };
    }
  }
  throw new Error('puzzle');
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  button.disabled = true;
  status.textContent = 'Checking you are not a robot…';
  try {
    const challenge = await (await fetch('challenge', { cache: 'no-store' })).json();
    const proof = await solve(challenge);
    status.textContent = 'Sending…';
    const data = Object.fromEntries(new FormData(form));
    const response = await fetch('report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...data, proof }),
    });
    if (response.ok) {
      const code = form.elements.followUp.value;
      form.reset();
      showPreview();
      status.textContent = code
        ? 'Thank you. Your report was sent. Ondea keeps its follow-up link in Settings → My reports.'
        : 'Thank you. Your report was sent.';
    } else {
      const { error } = await response.json().catch(() => ({}));
      status.textContent = error === 'title' ? 'Please write a summary (5 characters or more).'
        : error === 'details' ? 'Please add details (10 characters or more).'
        : 'The report could not be sent. Please try again later.';
    }
  } catch (e) {
    status.textContent = 'The report could not be sent. Check your connection and try again.';
  } finally {
    button.disabled = false;
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
