#!/usr/bin/env bash
set -euo pipefail
cat <<'NOTE'
The previous Homebrew-based key generation script has been retired to prevent accidental signing-key rotation.

The permanent LessonBell key already exists and was privately delivered to the owner.
No Homebrew, Java, or GitHub CLI installation is required.
Open https://github.com/Uduakobong-Udombat/git_test/settings/secrets/actions
Add a repository secret named LESSONBELL_SIGNING_BUNDLE_B64
Copy the contents of the private github-secret-LESSONBELL_SIGNING_BUNDLE_B64.txt.
Do not upload or commit the keystore or this secret value to the repository.

After the secret is set, trigger the "LessonBell permanent-signing release" workflow.
NOTE
