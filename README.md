# PolicyVault

**A free personal insurance tracker for Android.** Every policy in one place — health, term,
life, car, bike, accident, travel, home and office/group cover — with renewal reminders.
No account, no network access: everything stays on the phone.

**Download:** `PolicyVault.apk` from the [`app-latest` release](https://github.com/Akrishna87/insurance_tracker/releases/tag/app-latest).

## Features

- **All policy types**: health, term, life, car, bike, accident, travel, home, office/group and other
- **Office insurance**: employer/group name, covered members and TPA or claim contact, with no premium needed
- **Renewal reminders**: a notification 30, 7 and 1 day(s) before each renewal date (per-policy: 30/15/7 or off)
- **Dashboard**: total cover and yearly premium, what renews soon, expired policies, mix by type
- **Policies list**: search by insurer, policy number or member; filter by type
- **Mark renewed**: shifts a policy's dates a year forward in one tap
- **Backup**: export the whole vault as JSON, import it on another phone (duplicates skipped)
- **Light and dark themes**; Indian number formatting (lakh/crore)

All data lives in one JSON file in the app's private storage and nothing is ever uploaded.
Central-registry imports (like MF Central for mutual funds) may come later once a reliable
source exists; for now policies are added manually.

## How it works

```
core/    Plain Kotlin: the policy model, renewal and premium logic, JSON vault. Builds and tests anywhere with a JDK.
app/     The Android app: Compose UI, JSON storage, WorkManager reminders, notifications.
ci/      The emulator smoke test the CI runs on every build.
```

## Building

Open the project in Android Studio, or on the command line with the Android SDK set up:

```
./gradlew :app:assembleRelease
```

CI (GitHub Actions) runs the unit tests, builds a signed APK, walks through it on an emulator
and publishes it to the `app-latest` release on every push to `main`. Without signing secrets
the APK is signed with a debug key kept in the Actions cache, so each build installs over the
last one. To sign with your own key, set `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` as repository secrets.
