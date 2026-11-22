# Releasing Ondea

This page is for maintainers. For Google Play and the App Store, see [STORES.md](STORES.md).

## How a release works

A tag `vX.Y.Z` starts the [release workflow](../.github/workflows/release.yml). It:

1. checks that the tag matches `versionName`,
2. runs the tests and builds the release APK,
3. signs it with the release key (GitHub environment `release`),
4. fails if the APK asks for the `INTERNET` permission,
5. publishes `ondea-X.Y.Z.apk`, its `.sha256` file, `ondea-X.Y.Z.aab` (for Google Play) and the changelog as a GitHub release.

## Make a new release

1. In `app/build.gradle.kts`, increase `versionCode` by 1 and set `versionName`.
2. Add `fastlane/metadata/android/<lang>/changelogs/<versionCode>.txt` for each language (500 characters max).
3. Add the version to [CHANGELOG.md](../CHANGELOG.md).
4. Commit, push, then tag:
   ```sh
   git tag -a vX.Y.Z -m "Ondea X.Y.Z" && git push origin vX.Y.Z
   ```
5. Check the run in the **Actions** tab.

## Signing key

- The release key is `ondea-release.jks`, alias `ondea`, certificate `CN=Ondea`.
- SHA-256 of the certificate: `b943b8f3dcb132496097e4d4fbad38bc29736013f9211628c1cafa33a0838954`.
- `keystore.properties` (git-ignored) holds the passwords for local release builds:
  ```properties
  storeFile=ondea-release.jks
  storePassword=...
  keyAlias=ondea
  keyPassword=...
  ```
- GitHub environment `release` holds the same values as secrets: `KEYSTORE_BASE64` (`base64 -w0 ondea-release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Keep a backup of the key and its password outside the project. **If the key is lost, no update can ever be installed over existing installs.**

## Reproducible builds

F-Droid builds the app from source and compares the result with the signed GitHub APK. To keep builds reproducible, the release build has no VCS info and no ART baseline profile (see `app/build.gradle.kts`).

To check a release by hand:

```sh
git clone --branch vX.Y.Z https://github.com/0x0SegFault/ondea.git && cd ondea
./gradlew assembleRelease            # without keystore.properties: unsigned APK
apksigcopier compare ondea-X.Y.Z.apk --unsigned app/build/outputs/apk/release/app-release-unsigned.apk
```

## F-Droid

F-Droid accepts apps after 3 months of active development (Inclusion Policy, quality point 6). Planned submission: on or after 2027-02-21.

1. In [`fdroid/io.github.x0x0segfault.ondea.yml`](../fdroid/io.github.x0x0segfault.ondea.yml), set the build entry to the latest release: `versionName`, `versionCode`, and `commit` (full hash: `git rev-list -n 1 vX.Y.Z`).
2. Check the file with `fdroid lint` and `fdroid build -l io.github.x0x0segfault.ondea:<versionCode>` in a local copy of [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
3. Fork fdroiddata, add the file as `metadata/io.github.x0x0segfault.ondea.yml`, and open a merge request titled "New app: Ondea". Use the text in [`fdroid/merge-request.md`](../fdroid/merge-request.md).

After inclusion, F-Droid finds new `v*` tags automatically.

## Known notices

Lint (Android Gradle plugin 8.13) reads Kotlin metadata up to 2.2. With Kotlin 2.4 it prints "Module was compiled with an incompatible version of Kotlin". This is harmless: lint still runs. It goes away with the Android Gradle plugin 9 migration. Release builds skip lint (`checkReleaseBuilds = false`); CI runs it.

## Dependencies

Dependabot proposes updates once a month. It skips versions that need Android Gradle plugin 9 or Gradle 9.6+, because these need a planned migration. Kotlin plugins and libraries must use the same version: update them together.
