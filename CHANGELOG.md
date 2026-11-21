# Changelog

All notable changes to Ondea are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/).

## [1.2.0] – 2026-11-21

### Changed
- Your usual ovulation day and Ondea's estimate are both kept. Choose which one predicts. Ondea uses your day until it has learnt from a confirmed cycle.
- Creating a backup first asks where it goes: a drive or app (Proton Drive, Nextcloud, email…) through the share menu, or a file.
- Android Gradle plugin 8.13.2, Gradle 9.5.1, Kotlin 2.4.21, target SDK 36, updated AndroidX libraries.

## [1.1.0] – 2026-11-21

### Added
- Ovulation tests (LH): a positive test sets the ovulation estimate to the next day, until the temperature confirms it. The app shows the time range to ovulation (22–56 h) and the false-positive rate (about 7%).
- "How this date is set": tap the source of the date on Today to see each sign, which one is in use, and change your usual ovulation day.
- Setup can ask for your usual ovulation day (optional).
- Predictions learn your ovulation day from confirmed cycles; the predicted window opens before the earliest recent ovulation.
- Science and sources page in Settings, and [docs/SCIENCE.md](docs/SCIENCE.md).

### Changed
- Texts explain that the temperature rise confirms ovulation afterwards; mucus and ovulation tests come before it.

## [1.0.0] – 2026-11-21

First release.

### Added
- Daily logging of basal body temperature, period flow, cervical mucus, intercourse and notes.
- Ovulation confirmation with the 3-over-6 temperature rule and the cervical mucus double check (symptothermal method).
- Fertile window, peak days, ovulation and next period predictions, learnt from your cycles; optional usual ovulation day.
- Chance of conceiving by day (Wilcox et al., NEJM 1995).
- Cycle ring, temperature chart and calendar views.
- Encrypted on-device storage, app lock, screenshot blocking; no internet permission.
- Password-encrypted backups and CSV export.
- Optional discreet daily reminder.
- English, French, German, Spanish and Italian.
