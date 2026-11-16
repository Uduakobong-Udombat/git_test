#!/usr/bin/env bash
# LessonBell: one-time permanent signing setup, run locally on your Mac.
# Never uploads the private key to the public repository; only GitHub Actions Secrets.
set -euo pipefail
umask 077
REPO='Uduakobong-Udombat/git_test'
BRANCH='lessonbell-release'
KEYDIR="$HOME/Library/Application Support/LessonBell/signing"
KEYSTORE="$KEYDIR/lessonbell-release.p12"
PASSFILE="$KEYDIR/signing-password.txt"
SHAFILE="$KEYDIR/signing-sha1.txt"

if ! command -v gh >/dev/null 2>&1; then
  echo 'GitHub CLI is required. Install with: brew install gh' >&2
  exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
  echo 'OpenSSL was not found.' >&2; exit 1
fi
KEYTOOL=$(command -v keytool || true)
if [[ -z "$KEYTOOL" || ! -x "$KEYTOOL" ]]; then
  echo 'Java keytool is required. Install JDK 17, e.g. brew install openjdk@17' >&2
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo 'Sign into GitHub in this Terminal first: gh auth login --web'
  exit 1
fi

mkdir -p "$KEYDIR"
chmod 700 "$KEYDIR"
if [[ -f "$KEYSTORE" && -f "$PASSFILE" ]]; then
  echo 'Reusing your existing permanent signing certificate (no rotation).'
elif [[ -e "$KEYSTORE" || -e "$PASSFILE" ]]; then
  echo 'Only one of keystore/password exists. Refusing to rotate or guess credentials.' >&2
  echo "Check your secure backup in $KEYDIR" >&2
  exit 1
else
  echo 'Generating a one-time permanent signing certificate locally on this Mac...'
  openssl rand -hex 24 | tr -d '\n' > "$PASSFILE"
  chmod 600 "$PASSFILE"
  "$KEYTOOL" -genkeypair -noprompt \
    -storetype PKCS12 -keystore "$KEYSTORE" -alias lessonbell \
    -keyalg RSA -keysize 3072 -validity 10000 \
    -dname 'CN=LessonBell Wear OS, O=Personal Application, C=VN' \
    -storepass:file "$PASSFILE" -keypass:file "$PASSFILE"
  chmod 600 "$KEYSTORE"
fi

SHA1=$("$KEYTOOL" -list -v -alias lessonbell -keystore "$KEYSTORE" -storepass:file "$PASSFILE" | awk '/SHA1:/{print $2;exit}')
if ! [[ "$SHA1" =~ ^([0-9A-Fa-f]{2}:){19}[0-9A-Fa-f]{2}$ ]]; then
  echo 'Could not verify certificate SHA-1; nothing has been uploaded.' >&2
  exit 1
fi
printf '%s\n' "$SHA1" > "$SHAFILE"
chmod 600 "$SHAFILE"
echo "Permanent Android OAuth certificate SHA-1: $SHA1"
echo 'Uploading encrypted signing material into GitHub Actions Secrets...'
base64 < "$KEYSTORE" | tr -d '\n' | gh secret set LESSONBELL_KEYSTORE_B64 --repo "$REPO"
cat "$PASSFILE" | gh secret set LESSONBELL_STORE_PASSWORD --repo "$REPO"
printf '%s' "$SHA1" | gh secret set LESSONBELL_CERT_SHA1 --repo "$REPO"

echo 'Three signing secrets configured. Triggering the signed build...'
STAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
CONTENT=$(printf 'LessonBell signed release requested %s\n' "$STAMP" | base64 | tr -d '\n')
PATH_ON_REPO='lessonbell/release-request.txt'
SHA_EXISTING=$(gh api "repos/$REPO/contents/$PATH_ON_REPO?ref=$BRANCH" --jq '.sha' 2>/dev/null || true)
if [[ -n "$SHA_EXISTING" ]]; then
  gh api -X PUT "repos/$REPO/contents/$PATH_ON_REPO" -f message='Trigger signed LessonBell release' \
    -f branch="$BRANCH" -f content="$CONTENT" -f sha="$SHA_EXISTING" >/dev/null
else
  gh api -X PUT "repos/$REPO/contents/$PATH_ON_REPO" -f message='Trigger signed LessonBell release' \
    -f branch="$BRANCH" -f content="$CONTENT" >/dev/null
fi
cat <<DONE

Signing setup complete. Your key has NOT been committed to GitHub.

KEY FILE:     $KEYSTORE
PASSWORD:     $PASSFILE
SHA-1:        $SHA1

Keep a private encrypted backup of BOTH the keystore and password.
Do not upload them to chats or public repositories. Future releases MUST reuse this key.

Follow the build here:
https://github.com/$REPO/actions

Once the signed build passes, download the artifact named
LessonBell-v1.3.1-PERMANENTLY-SIGNED, unzip the two APKs, and use
LessonBell-install-permanent-upgrade.sh to migrate your old watch settings.

Register this new SHA-1 once in Google Cloud as another ANDROID OAuth client
for com.lessonbell.watch; do not delete your old clients.
DONE
