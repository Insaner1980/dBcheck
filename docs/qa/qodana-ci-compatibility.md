# dBcheck Qodana/CI compatibility QA

Date: 2026-07-24

Dependency versions refreshed: 2026-09-28. The earlier AGP 9.2.1 CI evidence is historical; Qodana 2026.1 failed to import the pull-request baseline during run 36444428943 on 2026-09-28 (job 109002919538), reporting AGP 9.3.1 above its supported 9.0.0. The green job masked this failure through continue-on-error. The configured community Android linter is now the stable 2026.2 image documented at https://www.jetbrains.com/help/qodana/jvm.html; a successful new analysis is required before merging this update.

Scope: Release-readiness QA for Qodana, AGP 9.4.1 compatibility, `continue-on-error` policy, and CI-status visibility. This document records evidence and gaps; it does not change product behavior.

Official docs checked before writing:
- Qodana GitHub Action: https://www.jetbrains.com/help/qodana/github.html
- Qodana JVM / Android linter family: https://www.jetbrains.com/help/qodana/jvm.html
- Qodana YAML configuration: https://www.jetbrains.com/help/qodana/qodana-yaml.html
- Qodana deployment options: https://www.jetbrains.com/help/qodana/deploy-qodana.html

## Current local state

- Project AGP: AGP 9.4.1 in `gradle/libs.versions.toml`.
- Qodana linter: `jetbrains/qodana-jvm-android:2026.2` in `qodana.yaml`.
- Qodana profile: `qodana.recommended`.
- Qodana include: `CheckDependencyLicenses`.
- Qodana action: `JetBrains/qodana-action` pinned to the v2026.2.1 commit in `.github/workflows/qodana.yml`.
- Docker: NOT AVAILABLE locally (`docker --version` not found).
- Qodana CLI: NOT AVAILABLE locally (`qodana --version` not found).
- Local Qodana run: NOT RUN because neither Docker nor Qodana CLI is available in this workspace.
- Previous CI Qodana run: PASS in PR #22 on 2026-07-06 with AGP 9.2.1.
- Current AGP 9.4.1 CI Qodana run: NOT RUN.

## Decision

`continue-on-error: true retained`.

Do not remove continue-on-error until all of these are true:

- A GitHub Actions Qodana run completes against this AGP 9.4.1 project without infrastructure/import failures.
- Qodana results are reviewed and either fixed or explicitly accepted.
- The job can be made blocking without turning AGP/tooling compatibility noise into a false release blocker.

The workflow now makes the non-blocking status visible in CI:

- Job name is `Qodana Analysis (non-blocking AGP 9.4 risk)`.
- A `Record Qodana compatibility risk` step writes to `GITHUB_STEP_SUMMARY`.
- The summary points maintainers back to this QA file before changing `continue-on-error`.

## Compatibility matrix

| Area | Evidence | Current status |
|---|---|---|
| AGP version | `gradle/libs.versions.toml` declares AGP 9.4.1. | Known project input. |
| Qodana linter | `qodana.yaml` uses `jetbrains/qodana-jvm-android:2026.2`. | Configured. |
| Qodana action | Workflow uses pinned `JetBrains/qodana-action` v2026.2.1 commit. | Configured. |
| Local execution | Docker and Qodana CLI are unavailable. | Local Qodana run: NOT RUN. |
| CI execution | PR #22 Qodana Actions log completed successfully on 2026-07-06 with AGP 9.2.1; AGP 9.4.1 is not yet CI-verified. | Current CI Qodana run: NOT RUN. |
| CI-status visibility | Job name and summary explicitly say Qodana is non-blocking for AGP 9.4. | Risk visible, still non-blocking. |

## CI-status policy

CI-status must not imply that Qodana is release-blocking while it is still marked `continue-on-error`.

Current policy:

- Qodana remains non-blocking.
- The non-blocking AGP 9.4.1 risk is visible in the status name and run summary.
- Release readiness cannot rely on Qodana alone until a real CI Qodana run is green without `continue-on-error`.
- Blocking checks remain the ordinary Android static checks, release build, CodeQL/Sonar/security workflows, and the final `lc`/`sc` user-run reports from Osa 99 - Final reports pass.

## Manual CI verification script

Run this on GitHub Actions, not just locally:

1. Trigger the `Qodana` workflow on a branch with current AGP 9.4.1.
2. Confirm the job is named `Qodana Analysis (non-blocking AGP 9.4 risk)`.
3. Confirm `Record Qodana compatibility risk` writes the AGP 9.4.1 warning to the run summary.
4. Inspect Qodana logs for Android import, Gradle sync, dependency resolution, and analysis completion.
5. If Qodana succeeds cleanly, open a follow-up change to remove `continue-on-error`.
6. If Qodana fails from AGP/tooling incompatibility, keep `continue-on-error` and track the failure as a release risk.

## Release risks and follow-up ownership

- Release risk: Local Qodana run: NOT RUN because Docker and Qodana CLI are unavailable.
- Release risk: Qodana remains non-blocking; the prior AGP 9.2.1 CI pass does not verify AGP 9.4.1 compatibility.
- Release risk: `continue-on-error: true retained`, so Qodana findings cannot be treated as release-blocking until a future stable run proves compatibility.
- Release risk: Qodana AGP 9.4.1 compatibility has no current CI pass, so the workflow remains non-blocking.
- Follow-up: Osa 99 - Final reports pass owns final local/user-run report review, including `lc`/`sc` outputs when the user runs them.

## Current update verification

Run 36446577784 used Qodana 2026.2 but still failed while importing base commit 02bec4b: Gradle 9.6.1 sources were not in that historical commit's verification metadata. The action even reported a successful scan step despite its logged nonzero exit, so workflow status alone is insufficient. PR analysis now uses `pr-mode: false` to analyze the complete current checkout, preserving strict dependency verification; a successful log/report at the latest commit remains required.

## Verified incompatibility on 2026-09-28

Qodana 2026.2 run [36457333058](https://github.com/Insaner1980/dBcheck/actions/runs/36457333058), commit d1c12bce34908c50a5af93d26de4ebaaa145050c, passed Gradle dependency verification after the source-artifact metadata corrections, then failed project import: AGP 9.4.1 is incompatible; the bundled Android plugin supports at most AGP 9.1.0. The logged scan exit code was 1 despite the successful workflow status. No analysis completion is claimed and this update remains blocked from merge under the current round approval conditions.

The stable linter documented by JetBrains is 2026.2: https://www.jetbrains.com/help/qodana/jvm.html . The vendor explains that AGP support is determined by the bundled Android plugin and cannot be resolved by project configuration: https://qodana-support.jetbrains.com/hc/en-us/articles/36480689906834-Qodana-Android-linter-Cannot-use-AGP-version-higher-than-9-0-0-alpha06 . That article has an older version bound; the current 9.1.0 bound above comes from this actual run.
