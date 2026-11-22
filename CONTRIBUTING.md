# Contributing to Ondea

Thanks for your interest! Ondea is a small, privacy-first project. Every contribution is reviewed by the maintainer before it is merged.

## Ways to help

- **Report a bug** or **suggest a feature**: [open an issue](https://github.com/0x0SegFault/ondea/issues/new/choose). Never include your personal health data.
- **Translate**: see [docs/TRANSLATING.md](docs/TRANSLATING.md). One command creates a new language, and another checks it.
- **Code**: fork the repository, create a branch, and open a pull request against `main`.

## Ground rules

These keep Ondea trustworthy and suitable for F-Droid:

1. **No network access.** The app must never request the `INTERNET` permission. CI checks this.
2. **No trackers, analytics, ads or proprietary libraries** (no Google Play Services, Firebase, etc.).
3. **Health data stays encrypted on the device.**
4. **Algorithm changes need evidence.** Predictions are based on the symptothermal method (Sensiplan) and peer-reviewed studies. Please cite your source and add unit tests in `app/src/test/`.
5. **All user-facing text goes in `strings.xml`.**

## Development

Requirements: JDK 17+ and the Android SDK (platform 36).

```sh
./gradlew testDebugUnitTest   # unit tests (algorithm, encryption, backups)
./gradlew lintDebug           # Android lint
./gradlew assembleDebug       # debug APK
```

The fertility logic lives in `app/src/main/java/app/ondea/domain/` and is plain Kotlin with no Android dependencies, so it is easy to test.

## Pull request review

- All pull requests need the CI checks to pass and an approval from the maintainer.
- Keep pull requests focused: one change per pull request is easier to review.
- By contributing, you agree that your contribution is licensed under the [GPL-3.0-or-later](LICENSE).

Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).
