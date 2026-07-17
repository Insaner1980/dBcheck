# dBcheck Permission/device QA matrix

Date: 2026-07-15

Scope: Osa 95 release-readiness QA for runtime permissions, foreground service types, device/API coverage, lockscreen behavior, and manual QA gaps. This document is a matrix, not a product-scope change.

Official Android docs checked before writing:
- Runtime permissions: https://developer.android.com/training/permissions/requesting
- Notification runtime permission: https://developer.android.com/develop/ui/views/notifications/notification-permission
- Foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Location permissions: https://developer.android.com/develop/sensors-and-location/location/permissions

## Current local device state

- `adb devices`: `Pixel_9_Pro` physical device on Android 17 / API 37 and `Pixel_9_Pro` emulator on Android 16 / API 36 were available.
- `emulator -list-avds`: `Pixel_10`, `Pixel_9_Pro`.
- Device smoke: PARTIAL PASS. The API 36 emulator completed the Meter, notification, CameraX, Sharesheet, Health Connect, and partial TalkBack checks recorded below.
- The physical API 37 compatibility warning found during this QA pass was fixed and the replacement build launched without the dialog; see `16 KB compatibility: PASS` below.
- Release risk: rows that remain `NOT RUN` or `PARTIAL` still require explicit device sign-off before release.

## Device smoke results (2026-07-15)

The debug APK was installed with `:app:installDebug`. App data was cleared only on the emulator; existing physical-device data was preserved.

| Release-critical path | Target and evidence | Status |
|---|---|---|
| Real microphone recording | Android 16 / API 36 emulator: denied `RECORD_AUDIO` showed the in-app permission state and did not start a measurement. After grant, the platform `AudioRecord`/service path produced a completed 1:24 session with 95 persisted samples and a non-zero live reading. | Real microphone recording: PASS. |
| Foreground-service promotion | API 36 Meter start promoted `MeasurementForegroundService` with `isForeground=true`, `startForegroundCount=1`, and foreground-service type `0x80` (`microphone`). Measurement continued while the app was backgrounded; notification Stop removed the service and opened the completed Session Detail. | Foreground-service promotion: PASS. |
| Notification permission denial | `POST_NOTIFICATIONS` was revoked before Meter start. Android displayed the runtime prompt; denial left permission false while the user-started microphone foreground service continued without a crash or silent start failure. A second run with permission granted displayed the recording notification and Stop control. | Notification permission denial: PASS. |
| CameraX preview/photo/video | Camera denial showed the expected in-app denied state. After grant, CameraX bound Preview, ImageCapture, and VideoCapture. Photo capture opened the Sharesheet. Video capture finalized a 10.17 s, 1280x720 H.264 MP4; `ffprobe` found one video stream and no audio stream. | CameraX preview/photo/video: PASS. |
| Android Sharesheet grant | Photo share used a FileProvider `content://` URI. Android Sharesheet displayed the image and the external Android Print receiver opened and rendered Page 1 of 1 without `SecurityException`, proving the temporary read grant was usable by a receiver. | Android Sharesheet grant: PASS. |
| Health Connect permission flow | With Health Connect installed on API 36, denial of Exercise write permission left Settings unconnected. Granting Exercise changed the state to partially connected; the separate heart-rate request then granted `READ_HEART_RATE` and changed the state to connected. The absent-provider case was not run. | Health Connect permission flow: PASS for installed-provider grant/deny; absent-provider case NOT RUN. |
| TalkBack navigation | The real Google TalkBack service was enabled; `dumpsys accessibility` reported it bound with touch exploration, and accessibility focus was visibly placed on the Settings action. Full spoken-output and double-tap navigation across the release paths was not completed. | TalkBack navigation: PARTIAL. |
| Play Billing test purchase | No Play-distributed build, authenticated license-tester account, or verified `dbcheck_pro` Play Console state was available. | License tester purchase: NOT RUN; see Osa 96 - Billing production QA. |
| Release AAB install | Release signing inputs were absent, so no signed Play-ready AAB could be installed or exercised. | Release AAB install: NOT RUN. |
| Android 16 KB compatibility | The original physical Pixel 9 / API 37 launch exposed the 4 KB-aligned legacy `libtask_audio_jni.so`. It was replaced with MediaPipe Tasks Audio 0.10.35. The rebuilt debug and unsigned release APKs passed `zipalign -P 16`; every ARM64 `PT_LOAD` alignment in the debug APK, release APK, and release AAB was `0x4000`. The physical device launched without the compatibility dialog, and an API 36 inference run loaded `libmediapipe_tasks_jni.so` plus `sound_detection/yamnet.tflite` without a native error. | 16 KB compatibility: PASS for built debug/release artifacts and physical debug launch. Signed release AAB install remains NOT RUN. |

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
| Meter measurement | `android.permission.RECORD_AUDIO`, `android.permission.POST_NOTIFICATIONS`, foreground service type `microphone` | `MeterScreen`, `RecordingRuntimeUi`, `MeasurementForegroundService` | `MeterStartupPermissionPolicyTest`, `MeasurementForegroundServicePolicyTest` | API 36 manual: mic deny/grant, notification deny/grant, foreground promotion, background continuation, notification Stop, and persisted samples PASS. Physical API 37 smoke BLOCKED by 16 KB warning. |
| Sleep recording | `android.permission.RECORD_AUDIO`, foreground service type `microphone` | `SleepSetupScreen`, `MeasurementForegroundService`, `AudioSessionManager.startSleepSession(...)` | `MeasurementForegroundServicePolicyTest`, sleep setup/recording tests | Manual: start Sleep from setup with keep-awake on/off, lock screen, target-duration stop. NOT RUN. |
| Passive monitoring | `android.permission.RECORD_AUDIO` | `PassiveMonitoringManager` | source-level permission gate in manager; Settings tests cover preference state | Manual: enable passive monitoring after mic grant, deny mic and verify no monitoring start. NOT RUN. |
| Ambient sound playback | `android.permission.POST_NOTIFICATIONS`, `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`, foreground service type `mediaPlayback` | `AmbientSoundPlaybackScreen`, `AmbientSoundPlaybackService` | `AmbientSoundPlaybackServicePolicyTest`, `AmbientSoundPlaybackViewModelTest` | Manual: Android 13+ notification deny blocks Play, grant starts mediaPlayback notification, notification Stop works. NOT RUN. |
| Camera overlay | `android.permission.CAMERA` | `CameraPermissionPolicy`, `CameraOverlayRoute` | `CameraPermissionPolicyTest`, `CameraOverlayShellContractTest`, `CameraXPreviewBindingContractTest` | API 36 manual: deny, preview, photo capture/share, and silent video capture PASS. Permanent-deny/settings and no-camera fallback NOT RUN. |
| Session location metadata | `android.permission.ACCESS_COARSE_LOCATION` only | Settings Data & Export, `AndroidSessionLocationCapturePort` | source gate checks coarse permission only; manifest excludes fine/background | Manual: request coarse from the Settings location action, verify captured metadata on completed session; deny and verify blank metadata. NOT RUN. |
| Health Connect noise sync | `android.permission.health.WRITE_EXERCISE` through Health Connect permission flow | `HealthConnectService`, `HealthSyncSection`, `HealthConnectManager` | Health Connect service/viewmodel tests and disclosure activities | API 36 installed-provider permission disclosure, deny, and grant PASS. Provider-absent and completed-session write verification NOT RUN. |
| Health Connect heart-rate overlay | `android.permission.health.READ_HEART_RATE` through Health Connect permission flow | `HealthConnectService`, `SessionDetailViewModel`, `HealthSyncSection` | Session Detail metadata tests cover missing/unavailable permission errors | API 36 installed-provider permission disclosure and grant PASS. Session Detail overlay data and unavailable-provider state NOT RUN. |
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
   - Open Settings > Data & Export and grant only `ACCESS_COARSE_LOCATION` from the session location action.
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

- Release risk: device-level permission flows are only partially signed off. Sleep, passive monitoring, ambient playback, session location, lockscreen visibility, Health Connect provider-absent/session-data behavior, and full TalkBack navigation remain open.
- Release risk: debug and unsigned release artifact 16 KB checks passed after the MediaPipe migration, but the signed AAB-derived install must still be exercised before release sign-off.
- Release risk: Play Billing test purchase and Release AAB install are NOT RUN because Play/license-tester access and release signing inputs were unavailable.
- Release risk: session location uses only best-effort `ACCESS_COARSE_LOCATION`; lack of a last-known provider must show blank metadata, not fake coordinates.
- Release risk: `com.android.vending.BILLING` is intentionally listed here only as a manifest inventory item. Functional purchase QA belongs to Osa 96 - Billing production QA.
- Release risk: Health Connect permissions are not normal Android runtime permissions; verify installed/unavailable and disclosure flows separately.
- Release risk: Meter notification denial passed on API 36; ambient playback notification denial is still NOT RUN.
