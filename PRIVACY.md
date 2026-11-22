# Privacy

Ondea does not collect, send or share any data.

## Your data

| | |
|---|---|
| Collected by the developer | Nothing |
| Shared with others | Nothing |
| Tracking, ads, analytics | None |
| Account | None |
| Where data is stored | Only on your phone, encrypted with a key held by the Android Keystore |
| Android cloud backup | Excluded |
| Delete your data | Settings → Erase all data, or uninstall the app |

Backups exist only when you create one. They are encrypted with your password, and you choose where they go. A CSV export is **not** encrypted: handle that file with care.

The daily reminder stores only "on/off" and the time, without encryption, so it can restart after a reboot. It contains no health data.

## Bug reports and crash reports

Ondea has no crash reporter and sends nothing by itself.

- **Settings → About → Report a problem / Suggest a feature** lets you choose how to send it. You see everything before you send it:
  - **Anonymous form**: opens a page in your browser. No account and no email. The service (Scaleway, Paris) does not read or store your IP address or any identifier, and reports go to a private repository, read by the maintainer before anything is made public. See [reports/README.md](reports/README.md).
  - **GitHub**: with your GitHub account; the report is public.
  - **Share as text**: with any app you choose.
  - The app version and phone model are filled in; you can delete them.
- **After a crash**, Ondea saves the technical error (no health data) on the phone. On the next launch it offers to report it; you read the text in the form before sending. "Dismiss" deletes it.

## Permissions

The release APK requests these permissions and no others. CI fails if this list changes.

| Permission | What it is for | When it is used |
|---|---|---|
| `POST_NOTIFICATIONS` | Shows the daily reminder | Android asks you when you turn the reminder on |
| `RECEIVE_BOOT_COMPLETED` | Sets the reminder again after a restart | Only if the reminder is on |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | Unlocks Ondea with fingerprint or face | Only if the app lock is on. Ondea never sees your fingerprint or face: Android only answers yes or no. `USE_FINGERPRINT` is for Android 8. |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Added by the AndroidX library. Keeps Ondea's internal messages private from other apps | Internal. It gives access to nothing. |

Ondea never requests internet, location, contacts, camera, microphone, phone, storage or account access. Files open only through the Android file picker, and only the file you choose.

The app shows this information in **Settings → Privacy → Permissions and data**.
