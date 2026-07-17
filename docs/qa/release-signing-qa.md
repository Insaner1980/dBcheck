# dBcheck Release signing QA

Date: 2026-07-15

Scope: Osa 97 release-readiness QA for Android release APK/AAB building, signing input handling, `apksigner` / `jarsigner` verification, signing lineage, and Play upload readiness. This document records evidence and gaps; it does not introduce new release secrets or product-scope changes.

Official docs checked before writing:
- Android app signing: https://developer.android.com/studio/publish/app-signing
- `apksigner` command-line verification: https://developer.android.com/tools/apksigner
- Upload an app bundle: https://developer.android.com/studio/publish/upload-bundle
- Play Console app bundle upload and release workflow: https://support.google.com/googleplay/android-developer/answer/9842756

## Current local state

- Signing secrets: NOT CONFIGURED locally.
- `DBCHECK_RELEASE_STORE_FILE`: missing.
- `DBCHECK_RELEASE_STORE_PASSWORD`: missing.
- `DBCHECK_RELEASE_KEY_ALIAS`: missing.
- `DBCHECK_RELEASE_KEY_PASSWORD`: missing.
- `DBCHECK_RELEASE_KEYSTORE_BASE64`: missing.
- `jarsigner`: available at `C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe`.
- `apksigner`: available through Android SDK build-tools, latest discovered `C:\Users\emmah\AppData\Local\Android\Sdk\build-tools\37.0.0\apksigner.bat`.
- Signed AAB build: NOT RUN because no release keystore or signing secrets were available.
- Release AAB install: NOT RUN because no signed Play-ready AAB was available.
- Play upload: NOT RUN because no signed Play-ready AAB and no authenticated Play Console upload session were available.
- 16 KB compatibility: PASS for the rebuilt debug APK, unsigned release APK, and release AAB native payload. MediaPipe Tasks Audio 0.10.35 replaced the 4 KB-aligned legacy Task Audio runtime; both APKs passed `zipalign -P 16`, every packaged ARM64 `PT_LOAD` alignment was `0x4000`, and the physical Pixel 9 / Android 17 debug launch no longer showed the compatibility dialog.
- Unsigned release APK/AAB build: RUN after this QA pass with `:app:assembleRelease :app:bundleRelease`.
- Partial signing input fail-fast: RUN. Supplying only `DBCHECK_RELEASE_STORE_FILE` failed configuration with `Release signing configuration is incomplete`.
- Version-controlled keystore scan: RUN. `rg --files` found no `.jks`, `.keystore`, `.p12`, or `.pfx` files.

## Signing configuration contract

| Area | Required state | Current evidence |
|---|---|---|
| Secret source | Signing reads Gradle properties or environment variables only. | `app/build.gradle.kts` reads `DBCHECK_RELEASE_STORE_FILE`, `DBCHECK_RELEASE_STORE_PASSWORD`, `DBCHECK_RELEASE_KEY_ALIAS`, `DBCHECK_RELEASE_KEY_PASSWORD`. |
| Partial secret handling | Partial signing input must fail fast. | Non-PR CI fails before Gradle if any required CI secret is missing. Gradle also throws `Release signing configuration is incomplete` if only some runtime signing inputs are present. |
| Keystore existence | Signed build must fail if the configured keystore file does not exist. | `hasReleaseSigning && !file(...).isFile` throws `Release signing keystore was not found`. |
| No committed keystore | Keystore and passwords must not be committed. | No hardcoded project `.jks` path is present in `app/build.gradle.kts`; workflow decodes the keystore from `DBCHECK_RELEASE_KEYSTORE_BASE64` only on non-PR events. |
| Release type | Release build keeps minify and resource shrink on. | `isMinifyEnabled = true`, `isShrinkResources = true`. |

## Artifact matrix

| Artifact | Command | Expected output | Verification | Current status |
|---|---|---|---|---|
| Unsigned release APK | `.\gradlew.bat :app:assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` when secrets are absent. | Build output exists; `apksigner verify --print-certs` is not expected to pass for unsigned artifact. | Unsigned release APK/AAB build: RUN. |
| Unsigned release AAB | `.\gradlew.bat :app:bundleRelease` | `app/build/outputs/bundle/release/app-release.aab` when secrets are absent. | `jarsigner -verify` is not expected to prove Play upload readiness without signing secrets. | Unsigned release APK/AAB build: RUN. |
| Signed release APK | CI non-PR build with all signing secrets. | `app-release.apk`. | `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`. | Signed APK verification: NOT RUN locally. |
| Signed release AAB | CI non-PR build with all signing secrets. | `app-release.aab`. | `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`. | Signed AAB build: NOT RUN. |
| Installed release AAB | Play internal/closed track or bundletool-generated APK set from the signed AAB. | Installed release package on a supported device. | Launch and complete the release device-smoke matrix, including Android 16 KB compatibility. | Release AAB install: NOT RUN; built release AAB native alignment: PASS. |
| Play upload | Play Console upload of signed AAB. | Release artifact accepted by Play. | Play Console accepts package, versionCode, signing lineage, and bundle validation. | Play upload: NOT RUN. |

## CI release workflow

`.github/workflows/release-build.yml` currently:

- Builds `:app:assembleRelease :app:bundleRelease` on PR without signing secrets and records that an unsigned release build was validated.
- On non-PR events, requires `DBCHECK_RELEASE_KEYSTORE_BASE64`, `DBCHECK_RELEASE_STORE_PASSWORD`, `DBCHECK_RELEASE_KEY_ALIAS`, and `DBCHECK_RELEASE_KEY_PASSWORD`; the workflow fails before the build if any value is missing.
- Exposes `DBCHECK_RELEASE_STORE_FILE` only through `$GITHUB_ENV`, not repository files.
- Runs signed artifact verification on every non-PR build after the required-secret preflight succeeds.
- Verifies signed APK with `apksigner verify --print-certs`.
- Verifies signed AAB with `jarsigner -verify`.

## Local verification results

- `.\gradlew.bat :app:assembleRelease :app:bundleRelease --no-daemon`: passed and produced `app-release-unsigned.apk` plus `app-release.aab`.
- `apksigner verify --print-certs app/build/outputs/apk/release/app-release-unsigned.apk`: failed as expected for an unsigned APK with `DOES NOT VERIFY`.
- `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`: reported `jar is unsigned`, so the local AAB is not a Play-upload-signed artifact.
- Partial signing input check with only `DBCHECK_RELEASE_STORE_FILE`: failed as expected with `Release signing configuration is incomplete`.
- Keystore scan: `rg --files` found no version-controlled `.jks`, `.keystore`, `.p12`, or `.pfx` files.

## Manual signed-release QA script

Run this only in a secure environment with release signing secrets available.

1. Confirm secrets are configured:
   - `DBCHECK_RELEASE_STORE_FILE`
   - `DBCHECK_RELEASE_STORE_PASSWORD`
   - `DBCHECK_RELEASE_KEY_ALIAS`
   - `DBCHECK_RELEASE_KEY_PASSWORD`
   - CI-only keystore material may be supplied as `DBCHECK_RELEASE_KEYSTORE_BASE64`.
2. Build signed artifacts:
   - `.\gradlew.bat :app:assembleRelease :app:bundleRelease --no-daemon`
3. Verify APK:
   - `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
   - Confirm signer certificate fingerprint matches the expected upload/signing lineage record.
4. Verify AAB:
   - `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`
   - Confirm the command exits successfully.
5. Play upload smoke:
   - Upload `app-release.aab` to the intended internal/closed test track.
   - Confirm Play accepts the package name, versionCode, target SDK, signing lineage, and bundle validation.
   - Do not publish broadly as part of this QA step unless release approval is explicit.

## Release artifact risks and follow-up ownership

- Release risk: signed build was not run locally because Signing secrets: NOT CONFIGURED.
- Release risk: Release AAB install: NOT RUN. The built release AAB native payload passes 16 KB checks, but a signed AAB-derived install must be verified separately.
- Release risk: `apksigner verify --print-certs` and `jarsigner -verify` were not run against signed release artifacts locally.
- Release risk: Play upload: NOT RUN; no authenticated Play Console upload validation was captured.
- Release risk: Signing lineage was not independently verified against a Play App Signing certificate or upload key fingerprint in this turn.
- Release risk: CI signed path depends on all required repository secrets being present on non-PR events; absence fails the workflow instead of producing an unsigned push artifact.
- Follow-up: Osa 98 - Qodana/CI compatibility owns the next release-readiness check after signing QA.
