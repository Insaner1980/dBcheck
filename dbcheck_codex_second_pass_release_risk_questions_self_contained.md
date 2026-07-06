# dBcheck Codex Second Pass Release Risk Questions

## 1. Current-state and stale-assumption audit

### 1. Compare `PROJECT.md` against the real code

```text
1. Compare `PROJECT.md` against the real code

Read `PROJECT.md` first, then inspect the codebase. Identify every claim in `PROJECT.md` that is outdated, exaggerated, missing, or contradicted by the actual code. Focus especially on features described as connected: Meter, Analytics, History, Session Detail, Settings, Health Connect, local backup, CSV/PDF/PNG exports, Pro entitlement, widget, foreground service, and hearing test flow.

Do not fix anything yet. Produce a table with: PROJECT.md claim, actual code evidence, status, risk, and recommended action.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 2. Find implementation drift after the first code review pass

```text
2. Find implementation drift after the first code review pass

Assume the previous comprehensive review questions were completed earlier. Search for signs that later edits created new inconsistencies: duplicated policies, changed route names, changed product IDs, changed permission flows, renamed resources, obsolete tests, stale comments, or old architecture assumptions.

Only report drift that is visible in code or project files. Do not assume an issue exists because the app is large.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 3. Audit TODOs, FIXME comments, temporary workarounds, and assistant residue

```text
3. Audit TODOs, FIXME comments, temporary workarounds, and assistant residue

Search the repository for `TODO`, `FIXME`, `HACK`, `temporary`, `later`, `not implemented`, `placeholder`, `mock`, `fake`, `debug`, `Claude`, `Codex`, and similar signs of unfinished or assistant-generated work. Classify each item as release blocker, safe internal note, obsolete comment, or needs manual review.

Do not delete comments blindly. Report only items that can confuse release readiness, user behavior, billing, privacy, Play Store policy, or future maintenance.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 4. Review naming consistency and product identity

```text
4. Review naming consistency and product identity

Inspect package names, application labels, notification titles, widget labels, export filenames, backup filenames, Health Connect notes, PDF/PNG/CSV labels, Play Store-facing strings if present, and privacy text. Verify that the app consistently appears as dBcheck and does not leak old names, wrong casing, obsolete project names, or internal technical names.

Report exact strings and files. Do not change branding unless the inconsistency is verified.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 2. End-to-end user journey reviews

### 5. Review the complete first launch to first completed session path

```text
5. Review the complete first launch to first completed session path

Trace a clean install path with no permissions, no sessions, no Pro state, no Health Connect setup, and no existing DataStore values. Follow the user from app open to starting measurement, granting or denying permissions, completing a session, reaching Session Detail, and sharing or returning home.

Look for missing loading states, permission loops, dead buttons, broken navigation events, empty data crashes, or misleading UI states. Separate code-verifiable issues from visual/device QA.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 6. Review the denied-permission user journey

```text
6. Review the denied-permission user journey

Trace what happens when the user denies microphone permission, denies notification permission, permanently denies microphone permission, revokes permission in Android Settings, or tries to start measurement after permission state changes. Include the service path, UI path, and any error events.

Report only verified loops, crashes, unclear states, or cases where measurement can start without the required permission.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 7. Review active measurement while navigating through the app

```text
7. Review active measurement while navigating through the app

Start from the code path where measurement is active. Inspect what happens if the user navigates to Analytics, History, Session Detail, Settings, Pro purchase UI, backup/export, or hearing test while measurement is running. Look for duplicate collectors, stale meter values, blocked actions that should be blocked, allowed actions that should not be allowed, or session state confusion.

Do not assume UI behavior. Trace state flows and event handlers.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 8. Review active measurement across app backgrounding, lock screen, rotation, and process death

```text
8. Review active measurement across app backgrounding, lock screen, rotation, and process death

Inspect code paths for activity recreation, app backgrounding, screen lock, foreground service survival, notification actions, process death, and interrupted-session recovery. The goal is not to prove every OEM behavior, but to identify what the code guarantees and what must be tested on a physical device.

Output a device QA checklist with exact scenarios and expected outcomes.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 9. Review reset, stop, failure, and completion semantics

```text
9. Review reset, stop, failure, and completion semantics

Trace every path that ends or abandons a measurement session: normal stop, reset, foreground notification stop, AudioRecord start failure, AudioRecord runtime failure, service destruction, process death recovery, and app restart recovery. Verify which paths should navigate to Session Detail, which should finish silently, and which should show errors.

Report mismatches between user expectation, persisted data, and navigation events.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 10. Review a Free user exploring every Pro entry point

```text
10. Review a Free user exploring every Pro entry point

As a Free user, inspect every UI entry point that advertises or opens a Pro feature: calibration, frequency weighting, lock-screen meter, PDF export, CSV export, widget, hearing test, naming/tags, spectral analysis, environment mix, monthly/yearly analytics, unlimited history, and heart rate overlay.

Verify that every path either shows a clear lock/upgrade path or safely blocks execution. Check direct navigation and service/use case paths, not only visible buttons.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 11. Review a Pro user upgrading and immediately using features

```text
11. Review a Pro user upgrading and immediately using features

Trace the purchase success event through Billing, DataStore, ProFeatureManager, ViewModels, navigation, widget refresh, settings state, locked cards, and active measurement preferences. Verify that a successful purchase enables Pro behavior without requiring app restart, unless restart is explicitly intended.

Report stale state, repeated purchase launches, lost Snackbar events, or features that remain locked after purchase.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 12. Review Pro to Free downgrade and debug Force Free behavior

```text
12. Review Pro to Free downgrade and debug Force Free behavior

Trace transitions from Pro to Free, purchase refresh returning false, pending purchase, cancelled purchase, billing unavailable, and debug Force Free toggling. Verify that UI, DataStore, AudioEngine preferences, spectrum, widget, exports, history direct-open, hearing test, and reports update safely.

Pay special attention to stale Pro data still visible after entitlement changes.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 3. Cross-feature invariants

### 13. Verify the one-report-data invariant

```text
13. Verify the one-report-data invariant

PROJECT.md describes `SessionReportCalculator` as the shared source for report data. Verify that Session Detail UI, PDF export, PNG sharing, CSV export where relevant, Health Connect notes, and any share text use compatible values and labels. Identify any place where the same metric is recalculated differently, rounded differently, named differently, or derived from a different source.

Report exact metric mismatches: LAeq/equivalent level, min/avg/max, LCpeak, TWA, dose, peak events, duration, weighting label, timestamps, and session metadata.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 14. Verify raw dB, weighted dB, and LCpeak cannot be mixed accidentally

```text
14. Verify raw dB, weighted dB, and LCpeak cannot be mixed accidentally

Audit the whole path from AudioRecord readings to persisted measurements, live UI, session summary, analytics, exports, PDF, PNG, Health Connect notes, and tests. Confirm whether raw RMS, selected weighted RMS, and C-weighted peak are consistently named and used.

Only report a bug if the code actually uses the wrong value or exposes an ambiguous label that could mislead users.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 15. Verify time, date, duration, and timezone consistency

```text
15. Verify time, date, duration, and timezone consistency

Inspect how timestamps are created, stored, queried, grouped, displayed, exported, and sent to Health Connect. Check session start/end, measurement timestamps, 24h hourly chart, 7 day history, 30 day trend, 12 month report, PDF/CSV date formatting, backup filenames, and relative widget time.

Report timezone or boundary bugs only when the code proves them. If ambiguity remains, propose targeted tests.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 16. Verify session metadata consistency across UI and exports

```text
16. Verify session metadata consistency across UI and exports

Inspect name, emoji, tags, slug generation, validation, truncation, duplicate-tag handling, PDF/PNG/CSV output, Session Detail display, History display, and restore behavior. Look for cases where metadata appears in one place but not another, breaks filenames, breaks CSV formatting, or leaks unsafe characters.

Do not widen validation unless a concrete failure path exists.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 17. Verify preference normalization across DataStore, UI, and runtime behavior

```text
17. Verify preference normalization across DataStore, UI, and runtime behavior

Inspect every preference that can affect behavior: theme, alerts, peak warnings, threshold, calibration, weighting, waveform style, refresh rate, lock-screen meter, Health Connect, heart rate overlay, debug force-free, and Pro status. Check invalid stored values, old values after updates, and Free/Pro effective policies.

Report only inconsistencies traceable from mapping, defaults, or consumers.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 18. Verify user-facing errors are mapped consistently

```text
18. Verify user-facing errors are mapped consistently

Search every error path in audio, service startup, billing, Health Connect, export, backup/restore, share, PDF, CSV, widget, database, and hearing test. Verify that errors become understandable user-facing text rather than silent failures, raw exceptions, internal class names, or misleading success states.

Report missing mappings and confusing messages with exact code paths.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 4. Platform and policy edge cases

### 19. Review target SDK 36 foreground-service risk from code and docs assumptions

```text
19. Review target SDK 36 foreground-service risk from code and docs assumptions

Inspect foreground service declarations, startup order, permission checks, notification creation, service stop action, and comments that mention Android versions. Do not browse from inside this task unless tooling allows it. From code alone, list what appears compliant, what is uncertain, and what must be verified against current official Android documentation and on a physical target SDK 36 device.

Do not claim policy failure unless code clearly violates a known requirement.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 20. Review notification permission and lock-screen privacy behavior

```text
20. Review notification permission and lock-screen privacy behavior

Inspect Android 13+ notification permission handling, notification channel creation, foreground notification behavior when permission is denied, threshold notifications, lock-screen visibility, custom RemoteViews, and fallback notification. Verify that private live dB content is not exposed where policy says it should be private.

Separate code findings from device/OEM behavior that needs manual verification.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 21. Review Health Connect representation risk

```text
21. Review Health Connect representation risk

The app writes noise sessions as `ExerciseSessionRecord` and intentionally skips hearing test writes. Inspect code and user-facing copy to verify that this representation is not described as a native noise exposure or audiometry integration. Check Settings, disclosures, Health Connect notes, reports, and docs in the repo.

Report mismatches as product-risk or copy-risk unless there is a code defect.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 22. Review Health Connect permission revocation and partial availability

```text
22. Review Health Connect permission revocation and partial availability

Trace the app when Health Connect is not installed, available but permissions are denied, permissions are later revoked, write succeeds but read fails, read succeeds but no samples exist, or Health Connect throws. Include Settings UI, session completion side effects, heart rate overlay, and PDF heart-rate section.

Report stale UI, hidden failures, or wrong session data.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 23. Review Play Billing production readiness boundaries

```text
23. Review Play Billing production readiness boundaries

Inspect product ID usage, purchase launch path, pending purchase handling, acknowledgement, already-owned handling, reconnect, refresh on resume, cached entitlement, debug behavior, release behavior, and user-facing purchase messages. Also list what cannot be verified without Play Console product setup and a signed/internal test build.

Do not invent Play Console problems. Mark those as manual verification.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 24. Review release signing and artifact verification flow

```text
24. Review release signing and artifact verification flow

Inspect Gradle signing config, release workflow, secret names, fail-fast behavior when partial secrets exist, generated APK/AAB artifacts, apksigner/jarsigner verification, and whether debug-only code is excluded from release. Identify whether the repository can produce a trustworthy release artifact without leaking secrets.

Report exact workflow or Gradle issues only.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 5. Export, backup, restore, and file trust

### 25. Review cancellation and failure behavior for every export and share path

```text
25. Review cancellation and failure behavior for every export and share path

Inspect PDF CreateDocument cancellation, PDF write failure, CSV export failure, PNG share generation failure, text share failure, unavailable Sharesheet, missing FileProvider URI, cache cleanup failure, and permission grant behavior. Verify that the UI does not show success after cancellation or failure.

Report exact event/state bugs and suggest focused tests.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 26. Review exported file content for user trust

```text
26. Review exported file content for user trust

Open the code that writes PDF, PNG, CSV, backup filenames, share text, and Health Connect notes. Check whether values are clearly labeled, units are present, weighting labels are correct, relative hearing-test limitations are visible where needed, and filenames are understandable.

Do not rewrite copy broadly. Report exact misleading labels or missing units.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 27. Review backup restore failure and rollback scenarios

```text
27. Review backup restore failure and rollback scenarios

Trace restore when the selected backup is invalid, old, corrupt, missing sidecar files, from a future schema, partially copied, or fails during replacement. Verify safety backup creation, validation, WAL/SHM cleanup, restart event, and what happens if restart fails or process is killed mid-restore.

Separate code-verifiable rollback issues from manual destructive QA.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 28. Review local backup copy and privacy expectations

```text
28. Review local backup copy and privacy expectations

Inspect UI strings, Settings labels, docs, and privacy files to ensure local backup is not described as cloud sync, Google Drive backup, encrypted backup, or automatic cross-device backup if the code does not implement that. Verify that users can understand where their data goes.

Report product copy mismatches and code privacy risks separately.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 29. Review FileProvider and export cache after repeated use

```text
29. Review FileProvider and export cache after repeated use

Trace repeated PDF/PNG/CSV/share operations over many sessions. Check cache location, cleanup age policy, filename collisions, URI permission flags, `ClipData`, `EXTRA_STREAM`, multi-file CSV sharing, and whether stale files can be shared accidentally.

Only report exposure if actual path and intent code prove it.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 30. Review large data export scalability

```text
30. Review large data export scalability

Assume a Pro user has many long measurement sessions and many measurement rows. Inspect database paging, memory allocation, PDF chart generation, PNG generation, CSV export, backup copy, and UI progress/error states. Identify any path that loads unbounded data into memory or blocks main thread.

If runtime proof is needed, propose a stress test with realistic row counts.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 6. Medical, acoustic, and product-risk second pass

### 31. Review every user-facing acoustic claim against implementation limits

```text
31. Review every user-facing acoustic claim against implementation limits

Search code/resources/reports/exports/share text for claims about accuracy, calibration, LAeq, LCpeak, dose, TWA, safe exposure, hearing health, professional reports, or scientific measurements. Compare each claim to implementation limits: phone microphone, app calibration, weighting filters, relative hearing test thresholds, and Health Connect representation.

Report exact risky text. Do not make legal claims.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 32. Review hearing test result interpretation boundaries

```text
32. Review hearing test result interpretation boundaries

Inspect hearing test setup, active test instructions, result scoring, rating labels, share card, result text, exports, and any warning/disclaimer strings. Verify that the app does not present relative app tone-output thresholds as calibrated clinical dB HL audiometry.

Classify findings as code defect, copy risk, or manual product decision.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 33. Review tone playback safety and user control

```text
33. Review tone playback safety and user control

Inspect tone generation, volume/amplitude assumptions, fade in/out, repeated taps, overlapping playback prevention, stop/release behavior, back navigation during tone playback, app backgrounding during tone playback, and user instructions around safe listening volume.

Do not change algorithm unless there is a verified lifecycle or safety issue.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 34. Review noise alert promise versus actual alert behavior

```text
34. Review noise alert promise versus actual alert behavior

Inspect Settings copy, threshold UI, peak warning UI, `NoiseAlertEvaluator`, notification update paths, and any alert-related strings. Verify that the app does not promise alerts, warnings, or safety protection that the implementation does not deliver consistently.

Report copy mismatch separately from code defect.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 7. Accessibility, localization, and layout second pass

### 35. Review complete TalkBack journeys, not only content descriptions

```text
35. Review complete TalkBack journeys, not only content descriptions

Inspect the expected TalkBack order and semantics for Meter, Analytics, History, Session Detail, Settings, Pro purchase card, hearing test setup/active/results, export/share actions, backup/restore, notification actions where relevant, and widget. Look for flows that are technically labeled but still unusable.

Separate code findings from manual screen reader QA.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 36. Review dynamic type and large-font resilience

```text
36. Review dynamic type and large-font resilience

Inspect Compose layouts for text scaling, clipped metric cards, fixed-height cards, chart labels, bottom navigation, NavigationRail, dialogs, bottom sheets, settings rows, Pro locks, hearing test controls, and export/share UI. Focus on layout assumptions that can be verified from code.

Do not demand visual perfection. Report likely clipping or inaccessible touch targets.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 37. Review localization readiness beyond hardcoded strings

```text
37. Review localization readiness beyond hardcoded strings

Inspect string resources, plurals, formatted strings, units, dates, percentages, CSV headers, PDF text, PNG text, notification text, widget text, and Health Connect notes. Verify that translation will not break because of concatenation, unescaped apostrophes, missing placeholders, ambiguous context, or hardcoded English outside resources.

Report representative exact files and strings.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 38. Review right-to-left, long-language, and narrow-screen risks

```text
38. Review right-to-left, long-language, and narrow-screen risks

Inspect layout and string usage for long translations, RTL mirroring assumptions, horizontal charts, icon plus text rows, settings controls, Pro cards, and report/share text. Since translations are not present, focus on code patterns that would break when translations are added.

Classify uncertain visual issues as manual QA.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 39. Review tablet, landscape, and foldable behavior for release confidence

```text
39. Review tablet, landscape, and foldable behavior for release confidence

Inspect adaptive navigation, width breakpoints, chart sizes, card grids, dialogs, bottom sheets, Session Detail, active hearing test controls, Settings, and History View All. Identify which layout issues can be proved from code and which require screenshots/device tests.

Output a small manual QA matrix by device class.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 8. Testing, CI, and release confidence

### 40. Build a prioritized missing-test list from current verified risks

```text
40. Build a prioritized missing-test list from current verified risks

Inspect existing tests first. Then propose only the missing tests that would catch real release risks. Prioritize integration-style unit tests, fake gateway tests, ViewModel state tests, and device/manual tests for things unit tests cannot verify.

Do not propose generic tests that duplicate existing coverage.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 41. Review existing tests for false confidence

```text
41. Review existing tests for false confidence

Inspect tests that cover billing, entitlement, audio/session lifecycle, foreground service policy, exports, backup/restore, Health Connect, report text, PDF chart rendering, widget state, and accessibility resources. Look for tests that simply mirror implementation, assert weakly, skip edge cases, or would pass despite broken behavior.

Report exact test names and why the test is weak.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 42. Review absence of `androidTest` instrumentation coverage

```text
42. Review absence of `androidTest` instrumentation coverage

The project currently relies on unit and screenshot tests. Inspect which release-critical behaviors cannot be trusted without instrumentation or physical-device tests: runtime permissions, foreground service, notification permission, Sharesheet/FileProvider, SAF CreateDocument, Play Billing test purchases, Health Connect permissions, widget update, and process death.

Create a prioritized manual/instrumentation test plan. Do not pretend unit tests can cover platform behavior they cannot cover.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 43. Review CI parity with local wrapper scripts

```text
43. Review CI parity with local wrapper scripts

Inspect local wrapper scripts, Gradle tasks, GitHub Actions workflows, SARIF uploads, lint/detekt/ktlint wiring, screenshot test validation, unit test tasks, dependency lock behavior, CodeQL, Semgrep, OWASP Dependency-Check, SonarCloud, Qodana, and release build workflows. Verify that local checks and CI checks are not silently different.

Report exact mismatches, skipped tasks, duplicate workflows, or continue-on-error risks.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 44. Review dependency locking and scanner maintenance risk

```text
44. Review dependency locking and scanner maintenance risk

Inspect dependency locking, version catalog, constraints, scanner plugins, suppression files if present, and CI commands. Identify whether security fixes could be accidentally undone or scanner outputs ignored. Do not suggest broad version upgrades unless there is a concrete repo-level reason.

Separate dependency freshness from verified build/security configuration issues.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 45. Review release artifact reproducibility

```text
45. Review release artifact reproducibility

Inspect whether a developer or CI can reproduce the same debug/release build inputs: Gradle wrapper, JDK version, Android SDK level, dependency locks, generated schema files, screenshot baselines, local reports ignored from git, and signing inputs. Identify fragile assumptions that could make release builds differ from tested builds.

Report only repository-verifiable fragility.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 9. Performance, robustness, and failure injection

### 46. Review main-thread and dispatcher usage under load

```text
46. Review main-thread and dispatcher usage under load

Inspect audio processing, repository writes, report calculation, PDF/PNG/CSV generation, backup/restore, Health Connect calls, widget updates, and startup. Look for code that may run heavy work on the main thread or use the wrong dispatcher under realistic data sizes.

Only report concrete dispatcher or blocking-call evidence.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 47. Review battery and CPU risk from active measurement

```text
47. Review battery and CPU risk from active measurement

Inspect measurement loops, UI refresh throttling, notification updates, spectral analysis, Room persistence cadence, widget updates, and flow collectors. Verify that Free and Pro paths do not accidentally do expensive Pro work when locked or backgrounded.

Report code-verifiable inefficiencies and propose profiling steps for device-only concerns.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 48. Review failure injection coverage with fake services

```text
48. Review failure injection coverage with fake services

Check whether the code and tests can simulate failures from Billing, Health Connect, AudioEngine, repositories, export writers, backup manager, FileProvider/share generator, notification helper, and widget updater. Identify where fake gateways exist and where failure behavior is hard to test.

Suggest minimal test seams only when they are justified by a verified untestable risk.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 49. Review crash-only edge cases

```text
49. Review crash-only edge cases

Search for edge cases that are not broad enough for the old crash review: empty report data with nonempty session, session deleted while detail screen is open, Pro revoked while export is in progress, Health Connect permission revoked while PDF heart-rate section is loading, backup restore while collectors are active, widget update after database restore, and orientation change during purchase or export.

Report only paths traceable in code. If uncertain, convert to targeted tests.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 50. Review cleanup after failed or cancelled operations

```text
50. Review cleanup after failed or cancelled operations

Inspect whether cancelled purchase, failed measurement start, failed export, cancelled CreateDocument, failed backup, failed restore, failed share, denied Health Connect permission, and failed widget update leave stale loading indicators, disabled buttons, temp files, active sessions, or incorrect success messages.

Report exact state cleanup issues.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

## 10. Final second-pass release decision

### 51. Produce a second-pass release readiness decision

```text
51. Produce a second-pass release readiness decision

Using only verified findings from this second-pass audit, produce a decision report:

- Can this build be submitted to internal testing?
- Can this build be submitted to closed/open testing?
- What blocks production release?
- What must be device-tested before any public release?
- What is safe to defer after v1.0?

Do not include speculative issues in the blocker list. Put uncertain items in a separate manual verification section.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 52. Create a minimal v1.0 hardening backlog

```text
52. Create a minimal v1.0 hardening backlog

After reviewing the current code, create a minimal backlog of only the fixes that materially improve v1.0 release safety. Group by release blocker, high risk, medium risk, and polish. For each item include the exact file/function, why it matters, smallest safe fix, and test or QA step.

Do not add nice-to-have refactors or feature ideas.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 53. Create a manual QA script for a real device

```text
53. Create a manual QA script for a real device

Based on the codebase, write a manual QA script for a real Android device. It must cover first launch, permissions, measurement, notification, lock screen, backgrounding, stop/reset, Session Detail, sharing, PDF, CSV, backup/restore, purchase state if test purchase is available, Health Connect, hearing test, widget, orientation, large font, and offline behavior.

The output should be practical, with expected results and pass/fail notes. Do not claim the tests passed.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```

### 54. Create a Play Store pre-submission checklist from the code

```text
54. Create a Play Store pre-submission checklist from the code

Create a Play Store pre-submission checklist grounded in this repository: permissions, Data safety form inputs to verify, privacy policy coverage, billing product setup, release signing, target SDK behavior, medical/acoustic disclaimers, screenshots/copy consistency, Health Connect disclosure, foreground service justification, and account/data deletion requirements if applicable.

Separate code-verified items from Play Console items that must be checked manually.

Required rules for this prompt:

- Do not invent issues. Report only what you can verify from the code, resources, Gradle files, workflows, tests, or documented project files.
- Treat `PROJECT.md` as a current-state reference, not as a promise. If code and `PROJECT.md` disagree, report the mismatch and quote the exact file paths involved.
- Assume the older comprehensive review questions were already run. Do not repeat generic findings unless there is new evidence, an unfixed issue, or a cross-feature interaction that the first pass likely missed.
- Do not refactor unrelated code. Do not change behavior unless a verified issue requires it.
- Do not commit.
- Before making changes, show the smallest safe diff. For large or risky changes, report first and wait for explicit approval.
- Separate verified code issues from device-test items, Play Console items, product copy risks, and manual QA recommendations.
- For every finding, include exact file paths, functions/classes/resources, why it matters, risk level, smallest safe fix, and suggested test or manual verification.

Recommended output format:

1. Verified issues
2. Needs manual/device verification
3. No issue found in these checked areas
4. Smallest safe fixes
5. Tests or QA steps to add
```
