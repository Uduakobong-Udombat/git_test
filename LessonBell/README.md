# LessonBell v1.3.1 — standalone Wear OS app, permanent signing

The package remains `com.lessonbell.watch`. Colorful full-screen Tiles, calendar sync, timetable, strong haptics and scheduled alerts are preserved.

**Sign a release once, then reuse the same key forever.** Previous v1.2 debug certificates cannot be recovered from SHA-1. Never uninstall the working app until its preferences are backed up.

## Signing process

On your Mac, download and run `LessonBell-setup-permanent-signing.sh`. It creates a private PKCS#12 keystore under `~/Library/Application Support/LessonBell/signing` and uploads its base64-encoded contents and password only to GitHub **Actions Secrets**, not the public repository.

Requires JDK 17 `keytool` and the GitHub CLI `gh` (authenticated). After secret setup, the script commits a release-request file to branch `lessonbell-release`, triggering the signed build.

GitHub Actions builds two APKs signed with that same key: `LessonBell-v1.3.1-migration.apk` (temporary debuggable for restoring data) and `LessonBell-v1.3.1-release.apk` (secure normal release). The artifact is named `LessonBell-v1.3.1-PERMANENTLY-SIGNED`.

Once signed APKs exist, use `LessonBell-install-permanent-upgrade.sh` alongside them. It backs up the existing preferences using Android's `run-as`, checks the backup **before any uninstall**, restores it into the migration build, verifies byte-for-byte integrity, and installs the final release over the migration build.

Register the SHA-1 displayed by the signing setup **once** as a new Android OAuth client for `com.lessonbell.watch`. Keep existing OAuth clients unchanged. Your Google account will require reauthorization on the new installation.

**Never lose the new keystore or password.** Keep a secure offline backup of both. Subsequent releases must reuse the same key; do not create new GitHub Actions debug certificates for distributable APKs.

The normal push workflow only compiles for validation. Signed release artifacts are created exclusively after explicit release-request commits, and fail closed if secrets are missing. The private key is removed from runners after signing. User hardware testing remains required.
