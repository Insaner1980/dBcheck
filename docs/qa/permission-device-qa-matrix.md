# dBcheck Permission/device QA matrix

Date: 2026-06-28

Scope: Osa 95 release-readiness QA for runtime permissions, foreground service types, device/API coverage, lockscreen behavior, and manual QA gaps. This document is a matrix, not a product-scope change.

Official Android docs checked before writing:
- Runtime permissions: https://developer.android.com/training/permissions/requesting
- Notification runtime permission: https://developer.android.com/develop/ui/views/notifications/notification-permission
- Foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Location permissions: https://developer.android.com/develop/sensors-and-location/location/permissions

## Current local device state

- `adb devices`: No connected device or running emulator.
- `emulator -list-avds`: `Pixel_10`, `Pixel_9_Pro`.
- Manual device smoke: NOT RUN in this turn because no ADB target was available.
- Release risk: all rows marked "manual" below still require a real device or running emulator before release sign-off.

## Manifest permission inventory

Current manifest permissions:
- `android.permission.RECORD_AUDIO`
- `android.permission.CAMERA`
- `android.permission.ACCESS_COARSE_LOCATION`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `android.permission.VIBRATE`
- `com.android.vending.BILLING`
- `android.permission.health.WRITE_EXERCISE`
- `android.permission.health.READ_HEART_RATE`

Explicitly excluded location scope:
- `android.permission.ACCESS_FINE_LOCATION` is not in the manifest.
- `android.permission.ACCESS_BACKGROUND_LOCATION` is not in the manifest.
- Foreground service `location` type is not used.

## Runtime permission surface matrix

| Surface | Manifest/runtime permission | Source owner | Automated evidence | Manual QA status |
|---|---|---|---|---|
| Meter measurement | `android.permission.RECORD_AUDIO`, `android.permission.POST_NOTIFICATIONS`, foreground service type `microphone` | `MeterScreen`, `RecordingRuntimeUi`, `MeasurementForegroundService` | `MeterStartupPermissionPolicyTest`, `MeasurementForegroundServicePolicyTest` | Manual: run first-launch mic deny/grant, Android 13+ notification deny/grant, start/stop/background smoke. NOT RUN. |
| Sleep recording | `android.permission.RECORD_AUDIO`, foreground service type `microphone` | `SleepSetupScreen`, `MeasurementForegroundService`, `AudioSessionManager.startSleepSession(...)` | `MeasurementForegroundServicePolicyTest`, sleep setup/recording tests | Manual: start Sleep from setup with keep-awake on/off, lock screen, target-duration stop. NOT RUN. |
| Passive monitoring | `android.permission.RECORD_AUDIO` | `PassiveMonitoringManager` | source-level permission gate in manager; Settings tests cover preference state | Manual: enable passive monitoring after mic grant, deny mic and verify no monitoring start. NOT RUN. |
| Ambient sound playback | `android.permission.POST_NOTIFICATIONS`, `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`, foreground service type `mediaPlayback` | `AmbientSoundPlaybackScreen`, `AmbientSoundPlaybackService` | `AmbientSoundPlaybackServicePolicyTest`, `AmbientSoundPlaybackViewModelTest` | Manual: Android 13+ notification deny blocks Play, grant starts mediaPlayback notification, notification Stop works. NOT RUN. |
| Camera overlay | `android.permission.CAMERA` | `CameraPermissionPolicy`, `CameraOverlayRoute` | `CameraPermissionPolicyTest`, `CameraOverlayShellContractTest`, `CameraXPreviewBindingContractTest` | Manual: deny, rationale, permanent deny/settings, no-camera fallback, photo/video controls. NOT RUN. |
| Session location metadata | `android.permission.ACCESS_COARSE_LOCATION` only | `AndroidSessionLocationCapturePort` | source gate checks coarse permission only; manifest excludes fine/background | Manual: grant coarse, verify captured metadata on completed session; deny and verify blank metadata. NOT RUN. |
| Health Connect noise sync | `android.permission.health.WRITE_EXERCISE` through Health Connect permission flow | `HealthConnectService`, `HealthSyncSection`, `HealthConnectManager` | Health Connect service/viewmodel tests and disclosure activities | Manual: Health Connect installed/uninstalled, permission grant/deny, Settings disclosure. NOT RUN. |
| Health Connect heart-rate overlay | `android.permission.health.READ_HEART_RATE` through Health Connect permission flow | `HealthConnectService`, `SessionDetailViewModel`, `HealthSyncSection` | Session Detail metadata tests cover missing/unavailable permission errors | Manual: Pro user with permission granted shows overlay; denied/unavailable shows inline error. NOT RUN. |
| Lockscreen meter | `android.permission.POST_NOTIFICATIONS`; notification visibility policy | `NotificationPrivacyPolicy`, `NotificationHelper`, `LockscreenMeterSection` | `SettingsViewModelDisplayPreferenceTest`, notification privacy policy coverage | Manual: private by default; public only with Pro + lockscreen meter + public opt-in; verify device lock screen. NOT RUN. |

## Device/API matrix

| Device/API | Why this row matters | Required manual checks |
|---|---|---|
| Android 11 / API 30 | Foreground service type constants are active from Android 11; no notification runtime permission. | Meter and Sleep `microphone` foreground service, Ambient `mediaPlayback`, camera grant/deny, lockscreen private/public visibility. |
| Android 12 / API 31-32 | Approximate location behavior and foreground service restrictions need coverage before Android 13 notification changes. | Coarse-only location metadata, no `ACCESS_FINE_LOCATION`, camera overlay, background/lockscreen measurement continuation. |
| Android 13 / API 33 | `POST_NOTIFICATIONS` runtime permission affects measurement and ambient playback notifications. | Deny/grant notification prompt for Meter and Ambient sound playback; ensure playback/recording paths fail visibly when notification permission blocks required notification. |
| Android 14 / API 34 | Foreground service type enforcement is stricter; Health Connect permission/disclosure paths matter. | Verify `microphone` and `mediaPlayback` foreground services start only from user action, Health Connect disclosure and denied states. |
| Android 15+ / API 35+ | Release-current behavior check for notification, FGS, camera, and lockscreen policies. | Run full smoke on the newest available emulator/device, including notification denial, lockscreen visibility, and service stop controls. |

## Manual QA script

Use a real device or a running emulator from `emulator -list-avds`.

1. Start target and install:
   - `adb devices`
   - `.\gradlew.bat :app:installDebug --console=plain --quiet`
   - `adb -s <serial> shell pm clear com.dbcheck.app`
2. Meter measurement:
   - Launch app.
   - Deny `RECORD_AUDIO`; verify Meter shows microphone permission prompt and no measurement starts.
   - Grant `RECORD_AUDIO`; start measurement.
   - On Android 13+, deny `POST_NOTIFICATIONS`; verify notification-dependent path is handled and no silent foreground failure occurs.
   - Grant `POST_NOTIFICATIONS`; verify foreground notification appears, backgrounding keeps measurement alive, Stop stops service.
3. Sleep recording:
   - Start `sleep/setup`.
   - Grant microphone when requested.
   - Verify keep-screen-awake on/off behavior and target-duration stop path.
4. Camera overlay:
   - Open camera overlay from Meter.
   - Deny `CAMERA`, retry, then grant.
   - Verify preview appears or camera-unavailable fallback appears on devices without a camera.
5. Session location metadata:
   - Grant only `ACCESS_COARSE_LOCATION`.
   - Complete a session and verify location metadata is present when provider has last-known location.
   - Revoke permission and verify metadata stays blank.
6. Ambient sound playback:
   - Open ambient playback.
   - On Android 13+, deny `POST_NOTIFICATIONS`; Play must not start playback.
   - Grant permission; Play starts `mediaPlayback` foreground notification and notification Stop stops playback.
7. Lockscreen meter:
   - Enable lockscreen meter.
   - Verify notification is private by default on the lock screen.
   - Enable public lockscreen opt-in as Pro and verify `VISIBILITY_PUBLIC` content appears on lock screen.
8. Health Connect:
   - Test with Health Connect installed and absent.
   - Grant and deny `WRITE_EXERCISE` and `READ_HEART_RATE`; verify Settings and Session Detail error states.

## Release risks and follow-up ownership

- Release risk: device-level permission flows are NOT fully signed off until at least one Android 13+ and one Android 14+ target complete the manual QA script.
- Release risk: no current ADB target was available in this turn, so no UIAutomator tree, screenshot, logcat, or lockscreen visual evidence was captured.
- Release risk: session location uses only best-effort `ACCESS_COARSE_LOCATION`; lack of a last-known provider must show blank metadata, not fake coordinates.
- Release risk: `com.android.vending.BILLING` is intentionally listed here only as a manifest inventory item. Functional purchase QA belongs to Osa 96 - Billing production QA.
- Release risk: Health Connect permissions are not normal Android runtime permissions; verify installed/unavailable and disclosure flows separately.
- Release risk: notification denial must be tested on Android 13+ for both measurement and ambient playback because both require visible foreground notifications.
