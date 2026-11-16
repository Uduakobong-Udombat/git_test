# LessonBell v1.2 — standalone Wear OS reminder app

A native, **watch-only** timetable app for the OPPO Watch X (Wear OS). No Android phone companion is needed. Default schedule was transcribed from the supplied school timetable (15 individual teaching periods, Monday–Thursday).

## Features
- Notifications **10, 5, and 2 minutes BEFORE each period** (including second periods). The next-alarm icon may be shown by Wear OS because alarm-clock scheduling is used for accurate delivery.
- **Strong vibration**: five 330ms pulses at requested amplitude `255` for the 2-minute reminder; three pulses at 5 minutes; two pulses at 10 minutes. Falls back to full on/off for hardware without amplitude control. **No app can override the watch's motor limits, DND, or hardware haptic settings.** Use the on-watch Test button.
- Native on-watch controls to skip/enable today, tomorrow, a selected date, or a full week. Stored offline and persistent across reboot.
- Offline weekly timetable seeded from screenshot and rebuilt after boot/timezone or time changes.
- Optional **on-watch calendar-provider** integration: requests `READ_CALENDAR`; reads accessible calendar events for the next 28 days, importing class titles matching `Computer Science`, `IGCSE CS`, `A Level CS`, `CS-12`, `CS-10`, `CS-9`, or lesson+grade. Matching start times replace neither the built-in periods nor duplicate an existing period. Automatically detects all-day entries in calendars named Holidays or explicitly labeled public/school holidays. A button turns auto-holiday skipping on/off.

## Important limitation: Google Calendar
This version syncs **through the watch's Android CalendarContract provider, if available**. Google Calendar's Wear OS application may not expose its events through a readable third-party provider on all builds. Thus Google Calendar sync is **experimental** rather than guaranteed. It does not authenticate to Google Calendar REST API directly: a production OAuth configuration is needed for that. On-watch manual holiday exclusions and built-in timetable work without Calendar access.

## Watch setup
1. Install the APK by Wear OS wireless debugging / `adb install -r app-debug.apk`.
2. Open LessonBell on the watch, grant **Alarms & reminders** and **Notifications**; grant Calendar access if offered and wanted.
3. Tap **Test strongest vibration**; adjust watch haptic settings and disable DND for audible / vibration alerts as needed.
4. Test the next upcoming scheduled reminder. This project has **not** been hardware-tested on your physical OPPO Watch X.

## Build
- GitHub Actions: workflow `.github/workflows/build-apk.yml` executes `gradle assembleDebug` and uploads the **debug-signed installable APK** artifact.
- Or local SDK: install Android SDK 35, JDK 17 and Gradle 8.9; run `gradle assembleDebug`.
- Debug signing is suitable for personal sideloading, not Play Store publication.

## Privacy
The default schedule and skip dates reside only on the watch. Reading calendar data happens locally through the operating system and no external servers are contacted by LessonBell.

## Follow-up opportunities
Editable timetable on the watch, direct Google Calendar OAuth login, an explicit alert grouping setting (first period only vs every period), and backup/export.


## Version 1.2 — two Wear OS Tiles
- **Next Lesson** tile: next upcoming period, class, room, rounded-up minute countdown, and Skip/Enable, Sync, Open actions.
- **Today’s Lessons** tile: up to 4 periods, disabled-date status, and the same quick controls.
- Tiles update when alarms or calendar cache change, plus a best-effort one-minute freshness interval. Wear OS may throttle Tile refresh; countdown is approximate, not guaranteed to tick each minute. Scheduled lesson alarms are independent of tile rendering.
- Tile controls act locally: Skip toggles today's skip state; Sync starts a watch-side Google API refresh when authorized, and Open launches LessonBell.
- Permissions and calendar settings are unchanged; updating the APK requires the same signing certificate. If Android rejects the upgrade, **back up local settings before uninstalling**; debug APKs produced by independent GitHub runners typically have different signing certificates.


## v1.3 colorful Tiles preview (not an in-place upgrade yet)
- Next Lesson Tile has a full-viewport dark navy background, large amber start time, mint countdown, violet room, and larger colorful buttons.
- Today's Lessons Tile shows up to five timetable rows in colored accents and uses more of the circular screen.
- **Signing blocker:** v1.2 was signed by a temporary GitHub-hosted debug keystore whose private key was not retained in the repository or artifacts. The SHA-1 alone cannot reproduce that signature. Do not uninstall v1.2 merely to test this preview. An in-place upgrade requires the original v1.2 signing keystore; absent that, a one-time migration to a persistently stored signing key would be needed.
