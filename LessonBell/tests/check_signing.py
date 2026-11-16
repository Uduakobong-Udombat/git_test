from pathlib import Path
p = Path('LessonBell/app/build.gradle').read_text()
w = Path('.github/workflows/build-apk.yml').read_text()
assert 'versionCode 5' in p and "versionName '1.3.1'" in p
assert "keyAlias 'lessonbell'" in p
assert 'signingConfig signingConfigs.permanent' in p
assert 'debuggable false' in p and 'debuggable true' in p
assert 'assembleMigration assembleRelease' in w
assert 'LESSONBELL_KEYSTORE_B64' in w
assert 'LESSONBELL_STORE_PASSWORD' in w
assert 'LESSONBELL_CERT_SHA1' in w
assert 'app-migration.apk' in w and 'app-release.apk' in w
assert 'Verify both APKs use the SAME permanent certificate' in w
print('PASS: permanent signing, migration variant, fingerprint verification')
