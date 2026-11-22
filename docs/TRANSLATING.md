# Translating Ondea

Ondea is available in English, French, German, Spanish and Italian. Help is welcome to add a language or improve one.

## What to translate

| Part | File |
|---|---|
| App texts | `app/src/main/res/values-<code>/strings.xml` |
| Store texts (F-Droid, Obtainium) | `fastlane/metadata/android/<code>/` |

English is the reference: `app/src/main/res/values/strings.xml`.

## Option 1: in your browser (Weblate)

When the project is on Weblate, a link appears here. You translate in a web page, and Weblate sends the changes for review. You do not need Git.

## Option 2: with a pull request

1. Fork the repository and clone it.
2. To add a new language, run:
   ```sh
   python3 scripts/new_language.py <code>
   ```
   Use an Android code: `pt`, `nl`, `pt-rBR` (Portuguese, Brazil), `zh-rCN`.
3. Translate the texts in the new files. Translate the text, not the `name="…"` attribute.
4. Check your work:
   ```sh
   python3 scripts/check_translations.py
   ```
5. Open a pull request. The same check runs automatically.

## Rules

- **Keep placeholders exactly:** `%d`, `%s`, `%1$s`, `%2$d`. They become numbers or dates. You can move them in the sentence.
- **Escape apostrophes and quotes:** write `\'` and `\"`. Example: `l\'ovulation`.
- **Percent sign:** where English has `%%`, write `%%`. Where English has a single `%`, write a single `%`.
- **Plurals:** keep an `<item quantity="other">`. Add `one`, `few`, `many` when your language needs them.
- **Keep it short and clear.** Use the words people use for their cycle in your language. Medical terms should match the fertility-awareness literature in your language (for example *glaire cervicale*, *Zervixschleim*).
- **Store limits:** title 50 characters, short description 80, changelog 500.
- **Do not translate** the app name "Ondea" or the study citations.

Missing strings are allowed: Android shows English for them. The check lists them as warnings.

## For the maintainer: set up Weblate

1. Create a project on [Hosted Weblate](https://hosted.weblate.org/hosting/) (free for libre projects).
2. Add a component **App**:
   - Repository: `https://github.com/0x0SegFault/ondea.git`, branch `main`
   - File format: *Android String Resource*
   - File mask: `app/src/main/res/values-*/strings.xml`
   - Monolingual base language file: `app/src/main/res/values/strings.xml`
3. Add a component **Store listing**:
   - File format: *Fastlane / Google Play metadata*
   - File mask: `fastlane/metadata/android/*`
   - Base: `fastlane/metadata/android/en-US`
4. Give Weblate push access through a pull request: in the component settings, set *Push URL* to your fork or use the Weblate GitHub app. Weblate then opens pull requests, and CI checks them.
5. Add the Weblate link to "Option 1" above and a translation badge to the README.
