Title: New app: Ondea

## Abstract for new app inclusion

### Please introduce the functions and usage of your app in summary, and also attach some screenshots.

Ondea is an offline fertility tracker for people who try to conceive. The user logs the basal body temperature every morning, plus period flow and cervical mucus. Ondea confirms ovulation with the 3-over-6 temperature rule and the cervical mucus double check of the symptothermal method (Sensiplan). It predicts the fertile window from past cycles and shows the chance of conception per day (Wilcox et al., NEJM 1995).

Screenshots: https://github.com/0x0SegFault/ondea/tree/main/fastlane/metadata/android/en-US/images/phoneScreenshots

### Please elaborate why you wrote this app. Please highlight the unique functions it provides over other similar apps in F-Droid.

I wanted a temperature-based tracker for conception that keeps health data private. Compared with other cycle apps:

- Focus on conception: confirmed ovulation, peak days, and conception chance per day from published data.
- Combines temperature and cervical mucus (double check), with a manual "usual ovulation day" option.
- No INTERNET permission. Data is encrypted with a key in the Android Keystore. Backups are encrypted with a user password.
- Reproducible build, signed by the developer.
- English, French, German, Spanish and Italian.

### AI disclosure

Ondea is AI-assisted: the code was written with an AI coding assistant. I set the requirements, review and test every change, and maintain the app. The app contains no AI or LLM features and downloads nothing. The algorithm has unit tests, and CI runs tests, lint and a check that the APK has no INTERNET permission.

## Checklist

(Copy the checklist from the fdroiddata "Default" template and tick each item.)
