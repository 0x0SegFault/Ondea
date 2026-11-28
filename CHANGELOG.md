# Changelog

All notable changes to Ondea are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Changed
- Today: after the mucus peak, a temperature rise that has started is shown ("high day 2 of 3") instead of "waiting for the rise".
- About and store texts: the disclaimer now says Ondea is not a medical device and does not diagnose, treat, cure or prevent any medical condition, and asks you to consult a healthcare professional. Store texts describe Ondea as a period and ovulation tracker and list the new features.
- README: new screenshots and how Ondea works.

## [1.4.0] – 2026-11-27

### Added
- Today: "Do your signs agree?" When the temperature rises and the mucus peak is known, Ondea says if they point to the same ovulation, and if it is earlier or later than your forecast day. If they are far apart, it asks you to check for disturbed readings.
- The tour has a new page: privacy and security (no internet, encryption, app lock, hidden content, encrypted backup, export).
- Science: SCIENCE.md and Settings → Science and sources explain every new rule (day-by-day estimate, late ovulation, sign check, what Ondea learns) with its source.

### Changed
- One ovulation day for the forecast. Only you change it. After a cycle with signs, Ondea shows what your signs point to and suggests that day. The "Ondea / My day" switch is removed.
- Licence: Ondea now uses the EUPL-1.2, the open-source licence of the European Union, instead of the GPL-3.0.
- The tour gives the temperature rise as 0.3 to 0.7 °C (Baker 2020).

### Fixed
- Anonymous report form: on some phones, Send seemed to do nothing. The anti-spam check is now much faster, the button shows its progress, and the result shows under it.
- The usual gap between mucus peak and ovulation is learnt from finished cycles only.

## [1.3.0] – 2026-11-27

### Added
- Short tour before the setup: what Ondea is, what to record, how to read the temperature chart, how Ondea helps. Skip it, or open it again in Settings → How Ondea works.
- Day by day estimate: a first high temperature, or creamy or egg-white mucus, moves the expected ovulation before it is confirmed. When the planned day passes without a sign while you record, the estimate moves later. The Today message and the ring always agree.
- After a cycle with a sign, Ondea compares the day it learnt with your setting ("You set day 14. Ondea finds day 17") and lets you choose.
- Ondea learns from every sign of a finished cycle (temperature, ovulation test or mucus peak), your usual low and high temperature, and your usual gap between mucus peak and ovulation.
- Reports: a preview of the issue in the app and on the form, an option to include anonymised crash details, and Settings → My reports to follow the status of anonymous reports.
- Reports: the anonymous form is now online (Scaleway, Paris).
- Chart: phase bands (period, follicular, ovulation, luteal), your usual levels, the link between mucus peak and temperature rise, and the typical research curve with an explanation of each phase.

### Changed
- Temperature entry: one wheel per digit, to 0.01 °C (0.02 °F), the resolution of a basal thermometer. You can also type it. The chart labels every 0.1 °C when there is room.
- Mucus noted on bleeding days no longer opens the fertile window.
- Reports: "Share as text" is removed. Send a report with the anonymous form or GitHub.

## [1.2.0] – 2026-11-21

### Changed
- Your usual ovulation day and Ondea's estimate are both kept. Choose which one predicts. Ondea uses your day until it has learnt from a confirmed cycle.
- New page Settings → Privacy → Permissions and data: each permission, why it is needed, and a data summary.
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
