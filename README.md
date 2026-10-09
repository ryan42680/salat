# Prayer Times (original app)

Prayer times + reliable notifications. Written from scratch; prayer times come from the open-source
Adhan library (com.batoulapps.adhan:adhan:1.2.1).

## Build
**Android Studio:** File > Open this folder, let Gradle sync, Run.
**No PC:** push this folder to a GitHub repo; the "Build APK" workflow produces a debug APK under Actions > Artifacts.

## How notifications stay on time
- One exact alarm (`setExactAndAllowWhileIdle`) for the next enabled prayer, which re-arms the following one first thing when it fires.
- Re-armed on boot, time/timezone change, app update, exact-alarm permission change, every app open, and every 6 h (WorkManager).
- In-app status rows for notification permission, exact alarms and battery restriction, plus "Test alarm (1 min)".

## Check before relying on it
- Pick the calculation method that matches your local mosque/ministry timetable and compare a few days.
- Not compiled or run in the environment that wrote it: expect to fix small build errors on first sync.
