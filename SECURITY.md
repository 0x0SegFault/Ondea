# Security policy

Ondea stores sensitive health data, so security reports are taken seriously.

## Reporting a vulnerability

**Please do not open a public issue.** Report it privately instead:
[Report a vulnerability](https://github.com/0x0SegFault/ondea/security/advisories/new) (GitHub private vulnerability reporting).

Please include the affected version, the steps to reproduce, and the impact. You will get a reply as soon as possible, and credit in the release notes if you wish.

## Supported versions

Only the latest release receives security fixes.

## Security design

- No `INTERNET` permission: the app cannot send data anywhere.
- Data encrypted at rest with AES-256-GCM; the key is generated in, and never leaves, the Android Keystore (StrongBox when available).
- Excluded from Android cloud backups and device-to-device transfer.
- Backups encrypted with the user's password (PBKDF2-HMAC-SHA256, 600,000 iterations, AES-256-GCM).
- Optional app lock (biometrics or device credential) and screenshot blocking.
