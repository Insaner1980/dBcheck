# dBcheck security context

dBcheck is an Android/Kotlin app for live sound-level measurement, hearing tests, exposure analytics, CSV/PDF/PNG export, Health Connect sync, local backup/restore, billing, and a home-screen widget.

High-value user data:
- Live microphone readings, weighted dB samples, session summaries, exposure history, and hearing-test thresholds.
- Health Connect noise-dose sessions and optional heart-rate reads.
- Exported CSV/PDF/PNG files shared through FileProvider.
- Local Room database backups under app-private storage.
- Billing and Pro entitlement state.

Important trust boundaries:
- `MainActivity` and Health Connect permission disclosure aliases are exported. Treat incoming intents and Health Connect actions as untrusted entry points.
- `MeasurementForegroundService` is the microphone foreground-service boundary. It must promote to foreground before starting audio capture, and failed foreground/audio start must not leave an active Room session.
- `AudioEngine` and `AudioSessionManager` own microphone capture, weighting filters, persistence cadence, and session completion.
- File sharing must use narrow FileProvider paths, temporary read grants, and ClipData for every content URI.
- Local backup/restore copies `dbcheck.db` and WAL/SHM-related state. Restore must validate backups, checkpoint Room, preserve a pre-restore backup, and restart the app after replacing the database.
- Health Connect sync should only request/write the documented minimal permissions: noise exposure modeled as exercise sessions and heart-rate reads only when Pro heart overlay is enabled.

Security expectations:
- `android:allowBackup` should stay false, and backup/data-extraction XML should exclude root app data.
- `android:usesCleartextTraffic` should not be enabled without a narrow documented exception.
- Logs must not include raw dB samples, hearing thresholds, session IDs, Health Connect values, backup paths, billing details, purchase tokens, exported file URIs, or user health data.
- `MeasurementForegroundService` should not start `AudioRecord` until runtime microphone permission and foreground-service promotion are confirmed.
- Dependency CVE findings are handled by OWASP Dependency-Check, not Deepsec.
