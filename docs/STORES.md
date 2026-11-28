# Publishing on Google Play and the App Store

This page is for maintainers. Store rules change: check each item in the store's own documentation before you submit.

## What is ready in the repository

| Item | Google Play | App Store |
|---|---|---|
| App build | `ondea-X.Y.Z.aab` on each GitHub release | Xcode archive from `ios/` (needs your Apple account) |
| Name, short and full description | `fastlane/metadata/android/<lang>/` | `ios/fastlane/metadata/<lang>/` (name, subtitle, keywords, description) |
| Icon | `images/icon.png` (512 px) | from the app (to add: 1024 px App Store icon) |
| Feature graphic (1024 x 500) | `images/featureGraphic.png` | not used |
| Screenshots | `images/phoneScreenshots/` | 6.9-inch iPhone screenshots from the iOS CI run |
| Privacy policy URL | `PRIVACY.md` | `PRIVACY.md` |
| Support URL | GitHub issues | GitHub issues |
| Languages | 5 | 5 |

## Google Play

### Account
Ondea is published from a **personal account**. Google requires an organization account for "health apps, such as medical apps and human subjects research apps" ([account types](https://support.google.com/googleplay/android-developer/answer/13634885)). Ondea is set up to stay out of that group:

- It is declared as **Period Tracking**, in the **Health and fitness** group of the health apps form. Do not select anything in the **Medical** group (for example "Reproductive and Sexual Health"), "Medical device apps" or "Health subjects research".
- It makes no medical claim: no diagnosis, no treatment, no contraception. The store description has the exact disclaimer that Google asks for ([Health Content and Services](https://support.google.com/googleplay/android-developer/answer/16679511)), and the app shows it in Settings → About.
- It is not a regulated medical device, does not use Health Connect and does not do research.

If a reviewer still asks for an organization account, reply with these points and the form answers below. If Google keeps the request, the options are an organization account (legal entity and a free D-U-N-S number) or F-Droid and IzzyOnDroid only.

1. Create a Google Play Console account (US$25, once). Google checks your identity with an official ID.
2. Your developer name and contact email are public. Your legal name is not shown on a personal account.
3. EU Digital Services Act: declare if you are a **trader**. A free app with no income is usually "non-trader"; traders must show an address and phone number.
4. New personal accounts must run a **closed test with at least 12 testers for 14 days in a row** before production. Google checks that the testers use the app.

### App setup forms (Play Console → Policy → App content)
| Form | Answer for Ondea |
|---|---|
| Privacy policy | `https://github.com/0x0SegFault/Ondea/blob/main/PRIVACY.md` |
| App access | No login needed |
| Ads | No ads |
| Content rating (IARC) | Answer the questionnaire: reproductive health information, no user content, no sharing |
| Target audience | 18+ recommended (sexual and reproductive health) |
| Data safety | No data collected, no data shared; data encrypted on the device; user can delete it in the app. The app has no internet permission. A problem report is sent by the user in the browser, through a separate form; the app sends nothing. |
| Health apps | **Period Tracking** (group "Health and fitness") only. Nothing in "Medical", not "Medical device apps", not "Health subjects research". |
| Store category | Health & Fitness (not Medical) |
| Review notes | No account or login needed. Period and ovulation tracker for people who want to conceive. No internet permission: all data stays on the phone. Not a medical device and not a contraceptive (disclaimer in the description and in Settings → About). Sources: Settings → About → Science and sources. |

### Signing
Play uses "Play App Signing". Choose **use my own key** and upload `ondea-release.jks` with Google's encryption tool (Play Console shows the command). The Play version then has the same signature as GitHub and F-Droid, so users can switch without losing data.

### Upload
Upload `ondea-X.Y.Z.aab` from the GitHub release to the closed test track first, then to production after the test period.

## App Store (Apple)

### Account and tools
1. Join the Apple Developer Program (US$99 per year).
   - **Individual:** your legal name is shown as the seller on the App Store.
   - **Organisation:** needs a legal entity and a D-U-N-S number; the organisation name is shown instead.
2. EU Digital Services Act: declare your trader status in App Store Connect (same rule as above).
3. You need a Mac with Xcode, or a CI job with an App Store Connect API key and your signing certificate stored as secrets.

### Before you submit
- The iOS app must be complete: Apple rejects apps that look unfinished. Finish the calendar, chart, backups and settings first.
- Add a 1024 px App Store icon to the iOS app.

### App Store Connect
| Item | Answer for Ondea |
|---|---|
| Bundle ID | `io.github.x0x0segfault.ondea` |
| Category | Health & Fitness (no secondary category: Ondea makes no medical claim) |
| App Privacy | Data Not Collected |
| Age rating | Answer the questionnaire: infrequent medical/treatment information |
| Export compliance | Uses only exempt encryption (data protection on the device). `ITSAppUsesNonExemptEncryption` is already `NO`. |
| Review notes | No account needed. Mention: not a contraceptive, no medical device claims, sources in Settings → About → Science and sources. |

Relevant review rules: 1.4.1 (medical accuracy: methods and sources are disclosed), 5.1.3 (health data: never used for ads, not collected).

### Upload
In Xcode: Product → Archive → Distribute App → App Store Connect. Test with TestFlight first, then submit for review.

## Licence note: EUPL and the App Store

Ondea uses the EUPL-1.2. As the only copyright holder, you can publish your own code in any store. Before you accept outside contributions to the iOS app, ask contributors to agree that their code can also be published through app stores (a contributor licence agreement).

Ask a lawyer if you are unsure.
