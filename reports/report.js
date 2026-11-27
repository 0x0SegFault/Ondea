'use strict';
// Validation of a report and its conversion to a GitHub issue.

const LIMITS = { title: 120, details: 5000, version: 40, device: 120 };
const KINDS = { bug: ['bug', 'anonymous'], feature: ['enhancement', 'anonymous'] };
// Same text as the preview in the app and on the form page.
const FOOTER = '_Sent anonymously._';

function clean(value, max) {
  return String(value ?? '').replace(/\r\n?/g, '\n').trim().slice(0, max);
}

/** @returns {{report?: object, error?: string}} */
// A follow-up code is chosen by the app (32 hex characters) and kept on the phone. Only its SHA-256 is
// written in the issue, so the code cannot be guessed from the repository, and nobody can tell which phone sent it.
const FOLLOW_UP = /^[0-9a-f]{32}$/;

function validate(input) {
  if (!input || typeof input !== 'object') return { error: 'invalid' };
  if (!Object.hasOwn(KINDS, input.kind)) return { error: 'kind' };
  if (input.followUp && !FOLLOW_UP.test(String(input.followUp))) return { error: 'followUp' };
  const report = {
    followUp: input.followUp ? String(input.followUp) : null,
    kind: input.kind,
    title: clean(input.title, LIMITS.title),
    details: clean(input.details, LIMITS.details),
    version: clean(input.version, LIMITS.version),
    device: clean(input.device, LIMITS.device),
    crash: input.crash === true || input.crash === 'true' || input.crash === 'on',
  };
  if (report.title.length < 5) return { error: 'title' };
  if (report.details.length < 10) return { error: 'details' };
  return { report };
}

function toIssue(report) {
  const prefix = report.kind === 'bug' ? 'Bug' : 'Idea';
  const lines = [
    report.details,
    '',
    '---',
    `App version: ${report.version || 'not given'}`,
    `Device: ${report.device || 'not given'}`,
    '',
    FOOTER,
  ];
  if (report.followUp) lines.push(`<!-- follow-up: ${followUpHash(report.followUp)} -->`);
  const labels = report.crash && report.kind === 'bug' ? [...KINDS[report.kind], 'crash'] : KINDS[report.kind];
  return { title: `${prefix}: ${report.title}`, body: lines.join('\n'), labels };
}

function followUpHash(code) {
  return require('node:crypto').createHash('sha256').update(`ondea-follow-up:${code}`).digest('hex');
}

/** Status for the app: received, published (with the public link the maintainer added) or closed. */
function statusOf(issue) {
  const link = /Public issue: (https:\/\/github\.com\/[^\s)]+\/issues\/\d+)/.exec(issue.body || '');
  if (link) return { status: 'published', public: link[1] };
  if (issue.state === 'closed') return { status: issue.state_reason === 'not_planned' ? 'declined' : 'closed' };
  return { status: 'received' };
}

module.exports = { validate, toIssue, LIMITS, followUpHash, statusOf, FOLLOW_UP, FOOTER };
