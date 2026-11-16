#!/usr/bin/env bash
# Safe one-time migration from v1.2 (temporary debug key) to v1.3.1
# (permanent signing key), retaining LessonBell SharedPreferences.
set -euo pipefail
umask 077
PKG=com.lessonbell.watch
DIRECTORY=$(cd "$(dirname "$0")" && pwd)
MIGRATION="$DIRECTORY/LessonBell-v1.3.1-migration.apk"
RELEASE="$DIRECTORY/LessonBell-v1.3.1-release.apk"
ADB=$(command -v adb || true)
if [[ -z "$ADB" ]]; then echo 'Install adb: brew install android-platform-tools' >&2; exit 1; fi
if [[ ! -s "$RELEASE" ]]; then echo "Missing $RELEASE" >&2; exit 1; fi

echo 'Connected Android devices:'
"$ADB" devices -l
DEVICES=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
if [[ -z "$DEVICES" ]]; then echo 'No online ADB device; connect your watch first.' >&2; exit 1; fi
if [[ $(printf '%s\n' "$DEVICES" | wc -l | tr -d ' ') -eq 1 ]]; then
  SERIAL="$DEVICES"
else
  printf 'Enter exact OPPO Watch X device identifier from above: '
  read -r SERIAL
  if ! printf '%s\n' "$DEVICES" | grep -Fxq "$SERIAL"; then
    echo 'Device not in the online list; refusing to continue.' >&2;exit 1
  fi
fi
A() { "$ADB" -s "$SERIAL" "$@"; }

if ! A shell pm path "$PKG" 2>/dev/null | grep -q '^package:'; then
  echo 'No old LessonBell installed. Installing the permanently signed release directly.'
  A install "$RELEASE"
  echo 'Done. Open LessonBell and allow notifications/precise alarms.'
  exit 0
fi

if [[ ! -s "$MIGRATION" ]]; then echo "Missing migration APK: $MIGRATION" >&2; exit 1; fi
BACKUPDIR="$HOME/Documents/LessonBell-backups/$(date '+%Y%m%d-%H%M%S')"
mkdir -p "$BACKUPDIR"
chmod 700 "$BACKUPDIR"
BACKUP="$BACKUPDIR/lessonbell_v1.xml"
echo 'Stopping the current app and backing up its saved settings using adb run-as...'
A shell am force-stop "$PKG"
if ! A exec-out run-as "$PKG" cat shared_prefs/lessonbell_v1.xml > "$BACKUP"; then
  echo 'Backup failed (old app may not be debuggable). NOT uninstalling anything.' >&2
  exit 1
fi
if [[ ! -s "$BACKUP" ]] || ! grep -q '<map' "$BACKUP"; then
  echo 'Backup was missing or not valid SharedPreferences XML. NOT uninstalling.' >&2
  exit 1
fi
if command -v xmllint >/dev/null 2>&1; then
  if ! xmllint --noout "$BACKUP"; then
    echo 'Backup XML validation failed. NOT uninstalling.' >&2
    exit 1
  fi
fi
chmod 600 "$BACKUP"
SUM=$(shasum -a 256 "$BACKUP" | awk '{print $1}')
echo "Verified private settings backup: $BACKUP"
echo "SHA-256: $SUM"
echo 'The previous signed APK cannot update in place because its key was lost.'
echo 'A one-time uninstall is required. Your verified settings backup stays on this Mac.'
printf 'Type MIGRATE to proceed with the one-time uninstall and restore: '
read -r YES
if [[ "$YES" != 'MIGRATE' ]]; then echo 'Cancelled. Existing watch app unchanged.'; exit 0; fi

A uninstall "$PKG"
A install "$MIGRATION"
A shell am force-stop "$PKG"
# The migration build is deliberately debuggable and signed with the permanent key.
# ADB shell -T streams XML directly to run-as without using external watch storage.
A shell -T "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/lessonbell_v1.xml'" < "$BACKUP"
if ! A exec-out run-as "$PKG" cat shared_prefs/lessonbell_v1.xml | cmp - "$BACKUP"; then
  echo 'Restore verification FAILED. The migration APK remains installed.' >&2
  echo "Backup remains at $BACKUP. DO NOT discard it." >&2
  exit 1
fi
echo 'Saved settings restored and byte-for-byte verified.'
A install -r "$RELEASE"
echo 'Permanent-signed release installed without removing the restored data.'
echo 'Recheck precise-alarm and notification permission on the watch.'
echo 'Reconnect Google Calendar and register the new certificate SHA-1 once.'
echo "Keep your settings backup at $BACKUP"
