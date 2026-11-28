# Ondea for iOS (in progress)

SwiftUI app on top of the shared Kotlin code in [`../shared`](../shared). Same rules, same data format and the same translations as Android.

Status: first version - setup, Today screen (ring, status, chance of conception, temperature, observations), encrypted storage, Face ID lock, basic settings. Calendar, chart, backups and the rest of the settings come next.

## Build on a Mac

Requirements: macOS, Xcode 16+, JDK 17, [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`).

```sh
./gradlew :shared:assembleOndeaSharedReleaseXCFramework   # shared Kotlin -> OndeaShared.xcframework
python3 scripts/ios_strings.py                            # translations from the Android strings
cd ios && xcodegen generate && open Ondea.xcodeproj
```

Run the app with the launch argument `--demo-data` to see sample cycles (not saved).

## Privacy on iOS

- No network code and no third-party SDK.
- Data file encrypted with AES-256-GCM (CryptoKit); the key is in the Keychain, this device only, available only when the device is unlocked.
- The data file is excluded from iCloud and iTunes backups and uses complete file protection.
- Optional lock with Face ID, Touch ID or the device passcode.

## CI

[`ios.yml`](../.github/workflows/ios.yml) builds and tests on a macOS runner. The build log and simulator screenshots are kept with each run for 14 days (Actions → the run → Artifacts).
