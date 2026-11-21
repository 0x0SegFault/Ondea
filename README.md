# Ondea

A private, offline Android app that finds your fertile days from your basal body temperature (BBT).

No account, no internet, no ads, no tracking. Your data stays encrypted on your phone.

## Features

- **Quick daily entry.** Type your waking temperature or adjust it with +/− buttons. You can also log period flow, cervical mucus, intercourse and a note.
- **Ovulation confirmation** from your temperature, using the symptothermal "3 over 6" rule (see [How it works](#how-it-works)).
- **Fertile window and peak days.** These are predicted from your history and confirmed once your temperature rises.
- **Next period forecast**, with a notice when your period is late. After 18 or more days of high temperatures, the app suggests a pregnancy test.
- **Temperature chart** for each cycle, showing the coverline, fertile window, ovulation, period and disturbed readings.
- **Calendar** with recorded and predicted days. Tap any past day to fill in or correct it.
- **Cycle control.** Start a new cycle on the first day of your period, or pause tracking at any time.
- **Optional daily reminder**, with neutral wording in case it shows on the lock screen.
- **°C or °F.**
- **Works without Google Play Services**, so it runs on LineageOS, GrapheneOS, /e/OS, CalyxOS and similar systems.

## Privacy and security

| | |
|---|---|
| Network | The app has **no INTERNET permission**. The manifest explicitly removes it, so no library can add it back. |
| Accounts / ads / analytics | None. |
| Google Play Services | Not used. The app has no Google Play Services or Firebase dependencies. |
| Storage | All data is kept in one app-private file, encrypted with **AES-256-GCM**. The key is generated inside the **Android Keystore** (hardware-backed, and StrongBox when the phone has it) and can never be exported. |
| Android cloud backup | Disabled (`allowBackup=false`, plus data-extraction rules that exclude everything). |
| App lock | Optional. Uses the phone's fingerprint, face unlock or screen lock (the framework BiometricPrompt). |
| Screenshots | Blocked by default, and the app's content is hidden in the recent-apps view. |
| Backups | Created only by you. They are encrypted with your password (PBKDF2-HMAC-SHA256 with 600,000 iterations, then AES-256-GCM). |

See [PRIVACY.md](PRIVACY.md).

### Backups and Proton Drive

Go to **Settings → Backup → Create encrypted backup**. The app asks for a password and then opens the Android file picker. You can save the file:

- on the phone or an SD card, or
- in any cloud app that appears in the file picker, such as Proton Drive, Nextcloud or Syncthing folders.

If your cloud app doesn't show up in the picker, save the backup to the phone and upload it with that app. The backup is encrypted with your password before it leaves Ondea, so the cloud provider can't read it.

To restore a backup, go to **Settings → Backup → Restore backup**.

## How it works

After ovulation, progesterone raises your resting temperature by about 0.2–0.5 °C until your next period.

**Confirming ovulation.** Ondea uses the "3 over 6" rule of the symptothermal method:

1. The *coverline* is the highest of the 6 valid readings before a given day.
2. If 3 consecutive valid readings are above the coverline, and the 3rd is at least **0.2 °C** above it, the temperature shift is confirmed.
3. Exception: if the 3rd reading is less than 0.2 °C above the coverline, a 4th reading above the coverline confirms the shift instead.
4. Ovulation is estimated as the day before the first high reading. The fertile window closes on the day the shift is confirmed.

Readings marked *disturbed* (illness, alcohol, a short night, a late measurement) are ignored.

**Predicting before the shift:**

- **Cycle length:** the average of your last 6 complete cycles once 2 or more are recorded. Until then, Ondea uses the length you entered.
- **Luteal phase** (from ovulation to the next period): learned from your cycles with a confirmed shift, or 14 days by default.
- **Fertile window:** the 5 days before predicted ovulation, ovulation day and the day after. The **peak days** are the 2 days before ovulation and ovulation day itself, which are the best days to try to conceive.

The algorithm lives in [`domain/`](app/src/main/java/app/ondea/domain) and is covered by unit tests.

### Background reading

- Frank-Herrmann P. et al. *The effectiveness of a fertility awareness based method to avoid pregnancy in relation to a couple's sexual behaviour during the fertile time.* Human Reproduction, 2007.
- Wilcox A.J., Weinberg C.R., Baird D.D. *Timing of sexual intercourse in relation to ovulation.* NEJM, 1995. This study is the basis for the 6-day fertile window.
- Arbeitsgruppe NFP. *Natürlich und sicher*, the Sensiplan reference handbook.

> **Disclaimer:** Ondea helps you understand your cycle and find your most fertile days. It is **not a medical device** and **not a contraceptive**. Ask a health professional for medical advice.

## Building

Requirements: JDK 17 or newer, and the Android SDK (platform 35).

```sh
git clone https://github.com/0x0SegFault/ondea.git
cd ondea
```

```sh
./gradlew testDebugUnitTest   # run the algorithm and crypto tests
./gradlew assembleDebug       # build app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # build a minified release APK
```

### Release signing

1. Create a key once, and keep it safe. Losing it means you can no longer publish updates.

   ```sh
   keytool -genkeypair -v -keystore ondea-release.jks -alias ondea -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Create `keystore.properties` in the project root. This file is git-ignored.

   ```properties
   storeFile=ondea-release.jks
   storePassword=...
   keyAlias=ondea
   keyPassword=...
   ```

3. Run `./gradlew assembleRelease`. The signed APK is written to `app/build/outputs/apk/release/`.

## Publishing

- **GitHub Releases:** attach the signed APK. Users can install it directly, or through [Obtainium](https://github.com/ImranR98/Obtainium).
- **F-Droid:** the project is ready to submit. It has no proprietary dependencies, includes [fastlane metadata](fastlane/metadata/android/en-US), and disables the dependency-info blob. To submit, open a merge request to [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
- **IzzyOnDroid:** an alternative to F-Droid that picks up the APKs from your GitHub releases.

## License

[GPL-3.0-or-later](LICENSE)
