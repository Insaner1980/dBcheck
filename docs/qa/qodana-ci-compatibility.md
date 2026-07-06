# dBcheck Qodana/CI compatibility QA

Date: 2026-06-29

Scope: Osa 98 release-readiness QA for Qodana, AGP 9.1.0 compatibility, `continue-on-error` policy, and CI-status visibility. This document records evidence and gaps; it does not change product behavior.

Official docs checked before writing:
- Qodana GitHub Action: https://www.jetbrains.com/help/qodana/github.html
- Qodana JVM / Android linter family: https://www.jetbrains.com/help/qodana/jvm.html
- Qodana YAML configuration: https://www.jetbrains.com/help/qodana/qodana-yaml.html
- Qodana deployment options: https://www.jetbrains.com/help/qodana/deploy-qodana.html

## Current local state

- Project AGP: AGP 9.1.0 in `gradle/libs.versions.toml`.
- Qodana linter: `jetbrains/qodana-jvm-android:2026.1` in `qodana.yaml`.
- Qodana profile: `qodana.recommended`.
- Qodana include: `CheckDependencyLicenses`.
- Qodana action: `JetBrains/qodana-action` pinned to the v2026.1.3 commit in `.github/workflows/qodana.yml`.
- Docker: NOT AVAILABLE locally (`docker --version` not found).
- Qodana CLI: NOT AVAILABLE locally (`qodana --version` not found).
- Local Qodana run: NOT RUN because neither Docker nor Qodana CLI is available in this workspace.
- CI Qodana run: NOT RUN in this turn because no GitHub Actions Qodana run output was available locally.

## Decision

`continue-on-error: true retained`.

Do not remove continue-on-error until all of these are true:

- A GitHub Actions Qodana run completes against this AGP 9.1.0 project without infrastructure/import failures.
- Qodana results are reviewed and either fixed or explicitly accepted.
- The job can be made blocking without turning AGP/tooling compatibility noise into a false release blocker.

The workflow now makes the non-blocking status visible in CI:

- Job name is `Qodana Analysis (non-blocking AGP 9.1 risk)`.
- A `Record Qodana compatibility risk` step writes to `GITHUB_STEP_SUMMARY`.
- The summary points maintainers back to this QA file before changing `continue-on-error`.

## Compatibility matrix

| Area | Evidence | Current status |
|---|---|---|
| AGP version | `gradle/libs.versions.toml` declares AGP 9.1.0. | Known project input. |
| Qodana linter | `qodana.yaml` uses `jetbrains/qodana-jvm-android:2026.1`. | Configured. |
| Qodana action | Workflow uses pinned `JetBrains/qodana-action` v2026.1.3 commit. | Configured. |
| Local execution | Docker and Qodana CLI are unavailable. | Local Qodana run: NOT RUN. |
| CI execution | No Qodana Actions log was available in this turn. | CI Qodana run: NOT RUN. |
| CI-status visibility | Job name and summary explicitly say Qodana is non-blocking for AGP 9.1. | Risk visible, still non-blocking. |

## CI-status policy

CI-status must not imply that Qodana is release-blocking while it is still marked `continue-on-error`.

Current policy:

- Qodana remains non-blocking.
- The non-blocking AGP 9.1.0 risk is visible in the status name and run summary.
- Release readiness cannot rely on Qodana alone until a real CI Qodana run is green without `continue-on-error`.
- Blocking checks remain the ordinary Android static checks, release build, CodeQL/Sonar/security workflows, and the final `lc`/`sc` user-run reports from Osa 99 - Final reports pass.

## Manual CI verification script

Run this on GitHub Actions, not just locally:

1. Trigger the `Qodana` workflow on a branch with current AGP 9.1.0.
2. Confirm the job is named `Qodana Analysis (non-blocking AGP 9.1 risk)`.
3. Confirm `Record Qodana compatibility risk` writes the AGP 9.1.0 warning to the run summary.
4. Inspect Qodana logs for Android import, Gradle sync, dependency resolution, and analysis completion.
5. If Qodana succeeds cleanly, open a follow-up change to remove `continue-on-error`.
6. If Qodana fails from AGP/tooling incompatibility, keep `continue-on-error` and track the failure as a release risk.

## Release risks and follow-up ownership

- Release risk: Local Qodana run: NOT RUN because Docker and Qodana CLI are unavailable.
- Release risk: CI Qodana run: NOT RUN in this turn because GitHub Actions output was not available locally.
- Release risk: `continue-on-error: true retained`, so Qodana findings cannot be treated as release-blocking until a future stable run proves compatibility.
- Release risk: Qodana AGP 9.1.0 compatibility is documented as unproven, not as passing.
- Follow-up: Osa 99 - Final reports pass owns final local/user-run report review, including `lc`/`sc` outputs when the user runs them.
