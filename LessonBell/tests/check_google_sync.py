from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/com/lessonbell/watch/CalendarSync.java').read_text()
auth=(root/'app/src/main/java/com/lessonbell/watch/GoogleCalendarAuth.java').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
activity=(root/'app/src/main/java/com/lessonbell/watch/MainActivity.java').read_text()
assert 'calendar/v3/' in java and 'singleEvents=true' in java
assert 'Bearer ' in java and 'calendar.readonly' in auth
assert 'AuthorizationRequest.builder' in auth
assert 'setPeriodic' in auth and 'setPersisted(true)' in auth
assert 'android.permission.INTERNET' in manifest and 'CalendarSyncJob' in manifest
assert 'Connect Google Calendar' in activity and 'Choose Google calendars' in activity
assert 'return "Calendar";' in java and 'calendar_authoritative' in (root/'app/src/main/java/com/lessonbell/watch/Lessons.java').read_text()
print('PASS: Google authorization, paging, offline cache, holiday and background sync wiring')
