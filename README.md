# Med Tracker (Android)

Minimal offline native Android medication tracker. No account, ads, analytics, or network permission.

## Open / build
1. Install Android Studio (JDK 17) and Android SDK 35.
2. Open this `MedTracker` folder as a project. Allow Gradle sync.
3. Select **Build > Build APK(s)**. Install the resulting debug APK on your Android phone.

## Features
- Medication name, strength, dose, reminder times, minimum hours between doses, maximum doses per rolling 24 hours.
- One-tap dose logging, early-dose warning and deliberate override.
- Date-based history and removal of incorrect records.
- Local scheduled reminders with notification permission on Android 13+.
- Rescheduling on restart and clock/timezone changes.

## Limitations and safety
- Reminders use Android's inexact alarms. Battery optimization, notification permission, Do Not Disturb, and system restrictions can delay/suppress them. Not a medical-grade reminder.
- No cloud backup or sync. Uninstalling clears records. No export yet.
- Scheduled reminders are fixed clock times; the app warns if a dose is too early but does not automatically adjust future clock times after a late dose.
- The app does not verify medical instructions, drug interactions, total mg, or different products containing the same active ingredient. Set intervals and dose limits from the medicine label/prescriber.
- Calendar shows recorded doses by date, not a clinically verified adherence score.
- The maximum doses/24h check counts entries, not tablets or milligrams. Only log one administered dose per entry.
- A user may override a warning, so this is an aid, not an enforced medication lockout.
