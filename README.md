# Champagne Atelier v2 v2 — classic dress-watch face for OPPO Watch X

An original, **free** watch face based on the third approved concept: champagne guilloché, black Breguet-inspired Arabic indices, brilliant blued-steel hands, a 6 o'clock small-seconds subdial, and restrained live health information. It uses **Watch Face Format v1** with no executable runtime and no paid watch-face app.

## What actually works

- Real-time **hour, minute and seconds hands**, with seconds displayed in the small 6 o'clock register (the seconds hand disappears in Always-on Display mode).
- Native Wear OS **heart rate** (`HEART_RATE`), **daily steps** (`STEP_COUNT`) and **watch battery** (`BATTERY_PERCENT`) readings.
- Dynamic **date** (`DAY`) in the small bevelled date window at ~4 o'clock.
- Black, reduced-detail **Always-on Display** (AOD) dial for lower power consumption.
- Fits a round watch; built for **Wear OS 4 (API 33) and higher**, including the original OPPO Watch X.

> **Important:** The numerical data comes from the Wear OS watch-face data providers. It is not guaranteed to match OHealth history or phone-collected steps at all times. When a provider reports 0 or no sensor data, the corresponding output may show 0. This source project does **not** require a Wear OS phone app or root access.

## Build a free APK without Android Studio, using GitHub Actions

1. Create a new empty GitHub repo (private is okay).
2. Unzip this project locally, then upload the **contents inside the project root**, including `.github/` (you should see `app/`, `scripts/`, `settings.gradle`, `build.gradle` at the repository root).
3. Push to `main`, or go to **Actions → Build Champagne Atelier v2 Watch X APK → Run workflow**.
4. After a successful run, open the run and download the artifact named `ChampagneAtelier-WatchX-APK`.
5. Unzip the artifact to find **`app-debug.apk`**. It is a debug-signed installer that can be directly sideloaded. No purchase is needed.

### Local build with Android Studio (alternative)

Open the project folder in Android Studio, set JDK 17 and Android SDK 35, then build the **debug** variant (`:app:assembleDebug`).

## Install on the Watch X from your Mac

1. Install Android platform tools (for `adb`) on your Mac if needed: https://developer.android.com/tools/releases/platform-tools .
2. On the watch enable developer options: **Settings → System → About → Versions**, then tap **Build number** seven times (wording differs slightly by firmware).
3. In **Developer options** enable **ADB debugging** and **Wireless debugging**. Connect Mac and watch to the same Wi-Fi network.
4. If the watch shows a *pairing* IP and port, run `adb pair WATCH_IP:PAIR_PORT` and enter the displayed code.
5. Run `adb connect WATCH_IP:DEBUG_PORT` using the **debugging** port shown on the watch (usually different from the pairing port).
6. Verify with `adb devices`, then install with `adb install -r /path/to/app-debug.apk`.
7. On the watch long-press the current face → **Add watch face** → **Champagne Atelier v2**.

If you prefer, run `Install-on-Mac.command` included in the project and follow the prompts instead of entering the ADB commands manually.
8. Turn **wireless debugging** off after installation.

The package name is `com.champagneatelier.watchx`, different from the previous designs. This installation will not overwrite OHealth or any other watch-face package.

## Assets and maintenance

- `app/src/main/res/drawable/dial.png`: clean premium dial, no fixed hands or sensor numbers.
- `hour.png`, `minute.png`, `seconds.png`: *independent* transparent blued-steel hands animated by Wear OS.
- `ambient.png`: black AOD dial.
- `app/src/main/res/raw/watchface.xml`: data and hand placements.
- `preview.png`: a realistic preview composited from those same resources, with **sample** numbers 78%, 72 bpm, 6,420 steps, date 12 and sample time 10:10:15.

## Build / validation status

The XML is structurally checked using `scripts/check_project.py`, which also validates resource names and hand pivot geometry. As the current execution environment lacks the Android SDK and has no external download connectivity, this project **has not been compiled into an APK or physically tested on the Watch X**. The free CI workflow is provided for the actual APK build and will clearly report any Android build failures.

Official WFF references: https://developer.android.com/codelabs/watch-face-format and https://developer.android.com/reference/wear-os/wff/common/attributes/source-type .


## v2 upgrade
- New edge-filling champagne guilloché dial and oversized numerals optimized for the OPPO Watch X screen
- Larger navy-blue main hands and an enlarged small-seconds register
- Larger dynamic battery, heart rate, step count and date data regions
- New on-watch name: **Champagne Atelier v2**
- New package ID: `com.champagneatelier.watchx.v2` so the previous v1 face stays installed for comparison; does not overwrite OHealth
- Build artifacts are debug-signed for free sideloading and need watch-side testing; actual OHealth sensor refresh depends on watch providers
- The preview contains demonstration metrics; the final dial resource contains no hard-coded numerical telemetry
