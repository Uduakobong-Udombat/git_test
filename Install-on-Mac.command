#!/bin/bash
# Champagne Atelier: install already-built APK over Wireless ADB.
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v adb >/dev/null 2>&1; then
  echo 'Android platform-tools are required.'
  echo 'Install with Homebrew: brew install android-platform-tools'
  echo 'Or get platform-tools from https://developer.android.com/tools/releases/platform-tools'
  exit 1
fi
APK="${1:-}"
if [[ -z "$APK" ]]; then
  read -r -p "Full path to app-debug.apk (drag the file here and press Return): " APK
  APK="${APK%\'}"; APK="${APK#\'}"; APK="${APK%\"}"; APK="${APK#\"}"
fi
if [[ ! -f "$APK" ]]; then
  echo "APK does not exist: $APK"
  exit 1
fi
echo ''
echo 'On watch: Settings > Developer options > Wireless debugging.'
echo 'Watch and Mac must be on same Wi-Fi.'
read -r -p 'Pair this Mac to the watch first (y/N)? ' NEED_PAIR
if [[ "${NEED_PAIR,,}" == "y" ]]; then
  read -r -p 'Enter WATCH_IP:PAIRING_PORT from Pair new device: ' PAIRADDR
  adb pair "$PAIRADDR"
fi
read -r -p 'Enter WATCH_IP:DEBUGGING_PORT (not the pairing port): ' WATCHADDR
adb connect "$WATCHADDR"
adb devices
adb install -r "$APK"
echo 'Installed. Long-press current watch face, choose Add, then Champagne Atelier.'
echo 'Disable Wireless debugging on the watch when finished.'
